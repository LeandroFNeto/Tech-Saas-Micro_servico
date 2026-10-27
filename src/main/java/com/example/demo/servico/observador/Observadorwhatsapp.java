package com.example.demo.servico.observador;

import com.example.demo.util.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Component
public class Observadorwhatsapp {

    private static final Logger log = LoggerFactory.getLogger(Observadorwhatsapp.class);

    public void logSucesso(String sessao, String jid, String msg) {
        log.info("[{}] Mensagem de {} processada ({})", sessao, MascaraLog.jid(jid), MascaraLog.tamanho(msg));
    }

    public void logFiltro(String sessao, String jid, String motivo) {
        log.info("[{}] Ignorada: jid={} motivo={}", sessao, MascaraLog.jid(jid), motivo);
    }

    public void logErro(String sessao, String jid, String local, Exception e) {
        if (e instanceof HttpStatusCodeException http) {
            log.warn("[{}] Erro em {} para {}: {} (HTTP {})", sessao, local, MascaraLog.jid(jid),
                    e.getClass().getSimpleName(), http.getStatusCode().value());
            return;
        }
        log.warn("[{}] Erro em {} para {}: {}", sessao, local, MascaraLog.jid(jid), e.getClass().getSimpleName());
    }
}
