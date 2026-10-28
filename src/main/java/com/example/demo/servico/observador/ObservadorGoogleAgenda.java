package com.example.demo.servico.observador;

import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.servico.AgendaNaoConfiguradaException;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ObservadorGoogleAgenda {

    private static final Logger log = LoggerFactory.getLogger(ObservadorGoogleAgenda.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final int MAXIMO_TENTATIVAS = 3;
    private static final long[] ESPERA_MS = {2000L, 5000L};
    private static final String MENSAGEM_FALHA =
            "Não consegui registrar sua reserva agora. O responsável vai confirmar com você em breve.";
    private static final String MENSAGEM_DATA_OCUPADA =
            "❌ Essa data acabou de ser reservada. Envie outra data no formato DD-MM-AAAA, por exemplo 16-09-2026.";

    private enum Resultado { SUCESSO, DATA_OCUPADA, FALHA }

    private final Map<String, ReentrantLock> bloqueios = new ConcurrentHashMap<>();

    @Autowired
    private ServicoReserva servicoReserva;

    @Autowired
    private ServicoMensagem servicoMensagem;

    @Autowired
    private ServicoGoogleagenda servicoGoogleagenda;

    @Autowired
    private GerenciadorSessao gerenciadorSessao;

    @Async
    @EventListener
    public void registarNoGoogle(EventoReservaConfirmada evento) {
        log.info("Reserva confirmada recebida; agendando no Google (empresa {})", evento.getEmpresa().getId());

        Resultado resultado = Resultado.FALHA;
        try {
            resultado = registrar(evento);
        } catch (RuntimeException e) {
            registrarFalha(evento, e);
        } finally {
            gerenciadorSessao.concluirRegistro(
                    evento.getNumeroWhatsapp(),
                    resultado == Resultado.DATA_OCUPADA ? EstadoUsuario.RESERVA_ESPERANDO_DATA : EstadoUsuario.INICIO);
        }

        try {
            servicoMensagem.enviarMensagemWPP(evento.getEmpresa(), evento.getNumeroWhatsapp(), mensagem(evento, resultado));
        } catch (RuntimeException e) {
            log.error("Erro ao enviar o resultado da reserva (empresa {}, pedido {}): {}",
                    evento.getEmpresa().getId(), evento.getIdPedido(), e.getClass().getSimpleName());
        }
    }

    protected void esperar(long milissegundos) throws InterruptedException {
        Thread.sleep(milissegundos);
    }

    private Resultado registrar(EventoReservaConfirmada evento) {
        LocalDate data = LocalDate.parse(evento.getDataReserva(), FORMATO_DATA);

        for (int tentativa = 1; tentativa <= MAXIMO_TENTATIVAS; tentativa++) {
            try {
                return tentar(evento, data, tentativa);
            } catch (Exception e) {
                boolean ultima = tentativa == MAXIMO_TENTATIVAS;
                if (!ehTransitoria(e) || ultima) {
                    registrarFalha(evento, e);
                    return Resultado.FALHA;
                }
                log.warn("Falha transitória ao registrar a reserva (empresa {}, pedido {}, tentativa {}): {}",
                        evento.getEmpresa().getId(), evento.getIdPedido(), tentativa, e.getClass().getSimpleName());
                try {
                    esperar(ESPERA_MS[tentativa - 1]);
                } catch (InterruptedException interrompida) {
                    Thread.currentThread().interrupt();
                    registrarFalha(evento, interrompida);
                    return Resultado.FALHA;
                }
            }
        }
        return Resultado.FALHA;
    }

    private Resultado tentar(EventoReservaConfirmada evento, LocalDate data, int tentativa) throws Exception {
        String calendarId = evento.getEmpresa().getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(evento.getEmpresa().getId());
        }

        ReentrantLock bloqueio = bloqueios.computeIfAbsent(
                evento.getEmpresa().getId() + "|" + data, ignorado -> new ReentrantLock());
        bloqueio.lock();
        try {
            if (tentativa > 1 && servicoReserva.eventoDoPedidoExiste(evento.getEmpresa(), evento.getIdPedido())) {
                return Resultado.SUCESSO;
            }
            if (!servicoGoogleagenda.dataEstaLivre(calendarId, data)) {
                return Resultado.DATA_OCUPADA;
            }
            servicoReserva.agendarNoGoogle(
                    evento.getEmpresa(),
                    evento.getDataReserva(),
                    evento.getNomeCliente(),
                    evento.getNumeroWhatsapp(),
                    evento.getIdPedido());
            return Resultado.SUCESSO;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 409) {
                return Resultado.SUCESSO;
            }
            throw e;
        } finally {
            bloqueio.unlock();
        }
    }

    private boolean ehTransitoria(Exception e) {
        if (e instanceof GoogleJsonResponseException google) {
            int status = google.getStatusCode();
            if (status >= 500 || status == 429) {
                return true;
            }
            return status == 403 && ehLimiteDeUso(google);
        }
        if (e instanceof HttpResponseException http) {
            return http.getStatusCode() >= 500 || http.getStatusCode() == 429;
        }
        return e instanceof IOException;
    }

    private boolean ehLimiteDeUso(GoogleJsonResponseException e) {
        if (e.getDetails() == null || e.getDetails().getErrors() == null) {
            return false;
        }
        return e.getDetails().getErrors().stream()
                .map(erro -> erro.getReason())
                .anyMatch(motivo -> "rateLimitExceeded".equals(motivo) || "userRateLimitExceeded".equals(motivo));
    }

    private void registrarFalha(EventoReservaConfirmada evento, Exception e) {
        log.error("Falha ao registrar a reserva na agenda (empresa {}, pedido {}): {}",
                evento.getEmpresa().getId(), evento.getIdPedido(), e.getClass().getSimpleName());
    }

    private String mensagem(EventoReservaConfirmada evento, Resultado resultado) {
        return switch (resultado) {
            case SUCESSO -> "✅ *Reserva confirmada!*\n\n"
                    + "📅 Data: " + evento.getDataReserva() + "\n"
                    + "📍 Sua reserva está registrada na nossa agenda.\n\n"
                    + "Agradecemos a preferência!";
            case DATA_OCUPADA -> MENSAGEM_DATA_OCUPADA;
            case FALHA -> MENSAGEM_FALHA;
        };
    }
}
