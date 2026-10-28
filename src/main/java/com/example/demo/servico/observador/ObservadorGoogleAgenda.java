package com.example.demo.servico.observador;

import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ObservadorGoogleAgenda {

    private static final Logger log = LoggerFactory.getLogger(ObservadorGoogleAgenda.class);

    @Autowired
    private ServicoReserva servicoReserva;

    @Autowired
    private ServicoMensagem servicoMensagem;

    @Async
    @EventListener
    public void registarNoGoogle(EventoReservaConfirmada evento) {
        log.info("Reserva confirmada recebida; agendando no Google (empresa {})", evento.getEmpresa().getId());

        try {
            servicoReserva.agendarNoGoogle(evento.getEmpresa(), evento.getDataReserva(), evento.getNomeCliente());

            String mensagemSucesso = "✅ *Reserva Confirmada com Sucesso!*\n\n" +
                    "📅 Data: " + evento.getDataReserva() + "\n" +
                    "📍 O seu lugar está garantido na nossa agenda.\n\n" +
                    "Agradecemos a preferência!";

            servicoMensagem.enviarMensagemWPP(evento.getEmpresa(), evento.getNumeroWhatsapp(), mensagemSucesso);

        } catch (Exception e) {
            log.error("Erro ao salvar na agenda do Google (empresa {}): {}", evento.getEmpresa().getId(), e.getClass().getSimpleName());
            servicoMensagem.enviarMensagemWPP(evento.getEmpresa(), evento.getNumeroWhatsapp(), "⚠️ Houve um pequeno erro ao registar na agenda digital, mas um assistente já vai verificar!");
        }
    }
}

