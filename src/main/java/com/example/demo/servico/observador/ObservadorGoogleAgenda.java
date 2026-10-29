package com.example.demo.servico.observador;

import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.ModoLembrete;
import com.example.demo.model.OrigemContato;
import com.example.demo.model.OrigemReserva;
import com.example.demo.model.Reserva;
import com.example.demo.model.SituacaoReserva;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.servico.AgendaNaoConfiguradaException;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoFuncionamento;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private record Desfecho(Resultado resultado, List<String> problemas) {
        static Desfecho de(Resultado resultado) {
            return new Desfecho(resultado, List.of());
        }
    }

    @FunctionalInterface
    private interface Tentativa {
        Desfecho executar(int numero) throws Exception;
    }

    private final Map<String, ReentrantLock> bloqueios = new ConcurrentHashMap<>();

    @Autowired
    private ServicoReserva servicoReserva;

    @Autowired
    private ServicoMensagem servicoMensagem;

    @Autowired
    private ServicoGoogleagenda servicoGoogleagenda;

    @Autowired
    private GerenciadorSessao gerenciadorSessao;

    @Autowired
    private ServicoFuncionamento servicoFuncionamento;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Async
    @EventListener
    public void registarNoGoogle(EventoReservaConfirmada evento) {
        log.info("Reserva confirmada recebida; agendando no Google (empresa {})", evento.getEmpresa().getId());

        Desfecho desfecho = Desfecho.de(Resultado.FALHA);
        try {
            desfecho = evento.isPorHora() ? registrarPorHora(evento) : registrar(evento);
        } catch (RuntimeException e) {
            registrarFalha(evento, e);
        } finally {
            if (desfecho.resultado() == Resultado.DATA_OCUPADA) {
                descartarDatasComProblema(evento, desfecho.problemas());
            }
            gerenciadorSessao.concluirRegistro(
                    evento.getNumeroWhatsapp(),
                    desfecho.resultado() == Resultado.DATA_OCUPADA
                            ? EstadoUsuario.RESERVA_ESPERANDO_DATA
                            : EstadoUsuario.INICIO);
            gravarReservaBot(evento, desfecho);
        }

        try {
            servicoMensagem.enviarMensagemWPP(evento.getEmpresa(), evento.getNumeroWhatsapp(), mensagem(evento, desfecho));
        } catch (RuntimeException e) {
            log.error("Erro ao enviar o resultado da reserva (empresa {}, pedido {}): {}",
                    evento.getEmpresa().getId(), evento.getIdPedido(), e.getClass().getSimpleName());
        }
    }

    protected void esperar(long milissegundos) throws InterruptedException {
        Thread.sleep(milissegundos);
    }

    private Desfecho registrar(EventoReservaConfirmada evento) {
        LocalDate data = LocalDate.parse(evento.getDataReserva(), FORMATO_DATA);
        return comTentativas(evento, numero -> tentar(evento, data, numero));
    }

    private Desfecho registrarPorHora(EventoReservaConfirmada evento) {
        Desfecho desfecho = comTentativas(evento, numero -> tentarPorHora(evento, numero));
        if (desfecho.resultado() == Resultado.FALHA) {
            compensar(evento);
        }
        return desfecho;
    }

    private Desfecho comTentativas(EventoReservaConfirmada evento, Tentativa tentativa) {
        for (int numero = 1; numero <= MAXIMO_TENTATIVAS; numero++) {
            try {
                return tentativa.executar(numero);
            } catch (Exception e) {
                boolean ultima = numero == MAXIMO_TENTATIVAS;
                if (!ehTransitoria(e) || ultima) {
                    registrarFalha(evento, e);
                    return Desfecho.de(Resultado.FALHA);
                }
                log.warn("Falha transitória ao registrar a reserva (empresa {}, pedido {}, tentativa {}): {}",
                        evento.getEmpresa().getId(), evento.getIdPedido(), numero, e.getClass().getSimpleName());
                try {
                    esperar(ESPERA_MS[numero - 1]);
                } catch (InterruptedException interrompida) {
                    Thread.currentThread().interrupt();
                    registrarFalha(evento, interrompida);
                    return Desfecho.de(Resultado.FALHA);
                }
            }
        }
        return Desfecho.de(Resultado.FALHA);
    }

    private Desfecho tentar(EventoReservaConfirmada evento, LocalDate data, int tentativa) throws Exception {
        String calendarId = evento.getEmpresa().getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(evento.getEmpresa().getId());
        }

        ReentrantLock bloqueio = bloqueioDoDia(evento, data);
        bloqueio.lock();
        try {
            if (tentativa > 1 && servicoReserva.eventoDoPedidoExiste(evento.getEmpresa(), evento.getIdPedido())) {
                return Desfecho.de(Resultado.SUCESSO);
            }
            if (!servicoGoogleagenda.dataEstaLivre(calendarId, data)) {
                return Desfecho.de(Resultado.DATA_OCUPADA);
            }
            servicoReserva.agendarNoGoogle(
                    evento.getEmpresa(),
                    evento.getDataReserva(),
                    evento.getNomeCliente(),
                    evento.getNumeroWhatsapp(),
                    evento.getIdPedido());
            return Desfecho.de(Resultado.SUCESSO);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 409) {
                return Desfecho.de(Resultado.SUCESSO);
            }
            throw e;
        } finally {
            bloqueio.unlock();
        }
    }

    private Desfecho tentarPorHora(EventoReservaConfirmada evento, int tentativa) throws Exception {
        String calendarId = evento.getEmpresa().getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(evento.getEmpresa().getId());
        }

        List<LocalDate> datas = evento.getDatas().stream()
                .map(texto -> LocalDate.parse(texto, FORMATO_DATA))
                .sorted()
                .toList();
        LocalTime inicio = LocalTime.parse(evento.getHorario());
        int intervalo = evento.getEmpresa().intervaloEfetivoMinutos();

        List<ReentrantLock> travados = new ArrayList<>();
        try {
            for (LocalDate data : datas) {
                ReentrantLock bloqueio = bloqueioDoDia(evento, data);
                bloqueio.lock();
                travados.add(bloqueio);
            }

            Set<Integer> existentes = new HashSet<>();
            if (tentativa > 1) {
                for (int posicao = 0; posicao < datas.size(); posicao++) {
                    if (servicoReserva.eventoPorHoraExiste(evento.getEmpresa(), evento.getIdPedido(), posicao)) {
                        existentes.add(posicao);
                    }
                }
                if (existentes.size() == datas.size()) {
                    return Desfecho.de(Resultado.SUCESSO);
                }
            }

            List<String> problemas = new ArrayList<>();
            List<ServicoFuncionamento.Janela> janelas = new ArrayList<>();
            for (int posicao = 0; posicao < datas.size(); posicao++) {
                LocalDate data = datas.get(posicao);
                ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(evento.getEmpresa(), data);
                janelas.add(janela);
                if (existentes.contains(posicao)) {
                    continue;
                }
                if (!janela.comporta(inicio)) {
                    problemas.add(data.format(FORMATO_DATA));
                    continue;
                }
                LocalTime fim = inicio.plusMinutes(janela.duracaoMinutos());
                if (!servicoGoogleagenda.horarioEstaLivre(calendarId, data, inicio, fim, intervalo)) {
                    problemas.add(data.format(FORMATO_DATA));
                }
            }
            if (!problemas.isEmpty()) {
                for (int posicao : existentes) {
                    servicoReserva.removerEventoPorHora(evento.getEmpresa(), evento.getIdPedido(), posicao);
                }
                return new Desfecho(Resultado.DATA_OCUPADA, problemas);
            }

            for (int posicao = 0; posicao < datas.size(); posicao++) {
                if (existentes.contains(posicao)) {
                    continue;
                }
                LocalDate data = datas.get(posicao);
                LocalTime fim = inicio.plusMinutes(janelas.get(posicao).duracaoMinutos());
                try {
                    servicoReserva.agendarPorHora(
                            evento.getEmpresa(), data, inicio, fim, evento.getNomeCliente(),
                            evento.getNumeroWhatsapp(), evento.getIdPedido(), posicao);
                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() != 409) {
                        throw e;
                    }
                }
            }
            return Desfecho.de(Resultado.SUCESSO);
        } finally {
            for (int i = travados.size() - 1; i >= 0; i--) {
                travados.get(i).unlock();
            }
        }
    }

    private void compensar(EventoReservaConfirmada evento) {
        String classeDaFalha = null;
        for (int posicao = 0; posicao < evento.getDatas().size(); posicao++) {
            try {
                servicoReserva.removerEventoPorHora(evento.getEmpresa(), evento.getIdPedido(), posicao);
            } catch (AgendaNaoConfiguradaException e) {
                return;
            } catch (Exception e) {
                classeDaFalha = e.getClass().getSimpleName();
            }
        }
        if (classeDaFalha != null) {
            log.error("Não foi possível desfazer eventos do pedido {} na agenda da empresa {} ({}); conferir a agenda",
                    evento.getIdPedido(), evento.getEmpresa().getId(), classeDaFalha);
        }
    }

    private void descartarDatasComProblema(EventoReservaConfirmada evento, List<String> problemas) {
        if (!evento.isPorHora() || problemas.isEmpty()) {
            return;
        }
        DadosReservaSessao reserva = gerenciadorSessao.getReserva(evento.getNumeroWhatsapp());
        if (reserva == null) {
            return;
        }
        for (String texto : problemas) {
            reserva.removerData(LocalDate.parse(texto, FORMATO_DATA));
        }
    }

    private ReentrantLock bloqueioDoDia(EventoReservaConfirmada evento, LocalDate data) {
        return bloqueios.computeIfAbsent(evento.getEmpresa().getId() + "|" + data, ignorado -> new ReentrantLock());
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

    private String mensagem(EventoReservaConfirmada evento, Desfecho desfecho) {
        if (evento.isPorHora()) {
            return mensagemPorHora(evento, desfecho);
        }
        return switch (desfecho.resultado()) {
            case SUCESSO -> "✅ *Reserva confirmada!*\n\n"
                    + "📅 Data: " + evento.getDataReserva() + "\n"
                    + "📍 Sua reserva está registrada na nossa agenda.\n\n"
                    + "Agradecemos a preferência!";
            case DATA_OCUPADA -> MENSAGEM_DATA_OCUPADA;
            case FALHA -> MENSAGEM_FALHA;
        };
    }

    private String mensagemPorHora(EventoReservaConfirmada evento, Desfecho desfecho) {
        return switch (desfecho.resultado()) {
            case SUCESSO -> "✅ *Reserva confirmada!*\n\n"
                    + (evento.getDatas().size() > 1 ? "📅 Datas: " : "📅 Data: ")
                    + String.join(", ", evento.getDatas()) + "\n"
                    + "⏰ Horário: " + evento.getHorario() + "\n"
                    + "📍 Sua reserva está registrada na nossa agenda.\n\n"
                    + "Agradecemos a preferência!";
            case DATA_OCUPADA -> "❌ Não consegui reservar este horário em: "
                    + String.join(", ", desfecho.problemas())
                    + ". Nada foi reservado. Envie outra data para substituir, no formato DD-MM-AAAA, "
                    + "por exemplo 16-09-2026.";
            case FALHA -> MENSAGEM_FALHA;
        };
    }

    private void gravarReservaBot(EventoReservaConfirmada evento, Desfecho desfecho) {
        if (desfecho.resultado() == Resultado.DATA_OCUPADA) {
            return;
        }
        try {
            if (!CriptografiaCampo.isConfigurado()) {
                CriptografiaCampo.avisarChavesNaoConfiguradasSeNecessario();
                return;
            }
            Empresa empresa = evento.getEmpresa();
            String numeroChave = WhatsappUtil.normalizarParaChaveEstado(evento.getNumeroWhatsapp());
            String numeroIndice = CriptografiaCampo.calcularIndice(numeroChave);
            String digitos = WhatsappUtil.extrairApenasNumeros(evento.getNumeroWhatsapp());
            String finalTelefone = (digitos != null && digitos.length() >= 4)
                    ? digitos.substring(digitos.length() - 4)
                    : null;

            Contato contato = contatoRepository.findByEmpresaIdAndNumeroIndice(empresa.getId(), numeroIndice)
                    .orElseGet(() -> {
                        Contato novo = new Contato();
                        novo.setEmpresa(empresa);
                        novo.setWhatsappId(evento.getNumeroWhatsapp());
                        novo.setNumeroIndice(numeroIndice);
                        novo.setFinalTelefone(finalTelefone);
                        novo.setPrimeiraConversaEm(LocalDateTime.now());
                        novo.setUltimaConversaEm(LocalDateTime.now());
                        novo.setOrigem(OrigemContato.CONVERSA);
                        return novo;
                    });
            if (evento.getNomeCliente() != null && !evento.getNomeCliente().isBlank()) {
                contato.setNome(evento.getNomeCliente());
            }
            contato.setUltimaConversaEm(LocalDateTime.now());
            contatoRepository.save(contato);

            String googleEventId = desfecho.resultado() == Resultado.SUCESSO
                    ? ServicoReserva.idEventoDoPedido(evento.getIdPedido())
                    : null;

            SituacaoReserva situacao = desfecho.resultado() == Resultado.SUCESSO
                    ? SituacaoReserva.AGUARDANDO_LEMBRETE
                    : SituacaoReserva.FALHA_AGENDA;

            LocalDate data = evento.getDataReserva() != null
                    ? LocalDate.parse(evento.getDataReserva(), FORMATO_DATA)
                    : (evento.getDatas() != null && !evento.getDatas().isEmpty()
                    ? LocalDate.parse(evento.getDatas().get(0), FORMATO_DATA)
                    : LocalDate.now());

            String horario = evento.getHorario() != null && !evento.getHorario().isBlank()
                    ? evento.getHorario()
                    : "Diária Completa";

            Reserva reserva = null;
            if (googleEventId != null) {
                reserva = reservaRepository.findByEmpresaIdAndGoogleEventId(empresa.getId(), googleEventId).orElse(null);
            }
            if (reserva == null) {
                reserva = new Reserva();
                reserva.setEmpresa(empresa);
                reserva.setGoogleEventId(googleEventId);
                reserva.setOrigem(OrigemReserva.BOT);
                reserva.setData(data);
                reserva.setHorario(horario);
            }
            reserva.setContato(contato);
            reserva.setTitulo("Reserva - " + (evento.getNomeCliente() != null ? evento.getNomeCliente() : ""));
            reserva.setSituacao(situacao);
            reserva.setAtualizadaEm(LocalDateTime.now());

            if (situacao == SituacaoReserva.AGUARDANDO_LEMBRETE) {
                reserva.setLembreteEnviarEm(calcularLembreteEnviarEm(empresa, data));
            }

            reservaRepository.save(reserva);
        } catch (Exception e) {
            log.error("Erro ao gravar reserva do bot (empresa {}, pedido {}): {}",
                    evento.getEmpresa().getId(), evento.getIdPedido(), e.getClass().getSimpleName());
        }
    }

    private LocalDateTime calcularLembreteEnviarEm(Empresa empresa, LocalDate dataReserva) {
        int diasAntes = empresa.getLembreteDiasAntes();
        String horarioStr = empresa.getLembreteHorarioEnvio();
        LocalTime horario = LocalTime.parse(horarioStr);
        LocalDate diaEnvio = dataReserva.minusDays(diasAntes);
        LocalDateTime momento = diaEnvio.atTime(horario);
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fuso);
        if (momento.isBefore(agora)) {
            return null;
        }
        return momento;
    }
}
