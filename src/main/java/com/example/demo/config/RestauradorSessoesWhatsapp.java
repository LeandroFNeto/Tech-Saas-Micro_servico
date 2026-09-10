package com.example.demo.config;

import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.ServicoWppConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reabre no WPPConnect as sessões que já tinham token/perfil salvos,
 * para o bot voltar conectado depois de uma queda do servidor.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "wppconnect.restaurar-sessoes", havingValue = "true", matchIfMissing = true)
public class RestauradorSessoesWhatsapp {

    private static final Logger log = LoggerFactory.getLogger(RestauradorSessoesWhatsapp.class);

    private final EmpresaRepository empresaRepository;
    private final ServicoWppConnect servicoWppConnect;

    public RestauradorSessoesWhatsapp(
            EmpresaRepository empresaRepository, ServicoWppConnect servicoWppConnect) {
        this.empresaRepository = empresaRepository;
        this.servicoWppConnect = servicoWppConnect;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restaurarSessoesExistentes() {
        for (Empresa empresa : empresaRepository.findAll()) {
            String sessao = empresa.getSessaoWhatsapp();
            if (sessao == null || sessao.isBlank()) {
                continue;
            }
            try {
                var status = servicoWppConnect.iniciarSessao(sessao);
                log.info("Sessão WhatsApp {} restaurada com status {}.", sessao, status.status());
            } catch (Exception e) {
                log.warn("Não foi possível restaurar a sessão WhatsApp {}: {}", sessao, e.getMessage());
            }
        }
    }
}
