package com.example.demo.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebhookSecretFilterTest {

    @Test
    @DisplayName("Segue no FilterChain quando x-webhook-token confere com o segredo configurado")
    void deveSeguirQuandoTokenValido() throws Exception {
        WebhookSecretFilter filtro = new WebhookSecretFilter("segredo-compartilhado");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/webhook/whatsapp");
        req.addHeader(WebhookSecretFilter.HEADER_WEBHOOK_TOKEN, "segredo-compartilhado");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Retorna 401 e bloqueia FilterChain quando x-webhook-token está ausente")
    void deveRetornar401QuandoTokenAusente() throws Exception {
        WebhookSecretFilter filtro = new WebhookSecretFilter("segredo-compartilhado");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/webhook/whatsapp");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    @DisplayName("Retorna 401 sem vazar segredo no log quando x-webhook-token está incorreto")
    void deveRetornar401SemVazarSegredoQuandoTokenIncorreto() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(WebhookSecretFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            WebhookSecretFilter filtro = new WebhookSecretFilter("segredo-real-123");
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/webhook/whatsapp");
            req.addHeader(WebhookSecretFilter.HEADER_WEBHOOK_TOKEN, "segredo-falso-999");
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filtro.doFilter(req, res, chain);

            assertThat(res.getStatus()).isEqualTo(401);
            verify(chain, never()).doFilter(req, res);
            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(logs).contains("Webhook WhatsApp recusado em /webhook/whatsapp");
            assertThat(logs).doesNotContain("segredo-real-123");
            assertThat(logs).doesNotContain("segredo-falso-999");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("Recusa tudo com 401 quando o segredo configurado está vazio ou em branco")
    void deveRecusarTudoQuandoSegredoConfiguradoVazio() throws Exception {
        WebhookSecretFilter filtro = new WebhookSecretFilter("   ");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/webhook/whatsapp");
        req.addHeader(WebhookSecretFilter.HEADER_WEBHOOK_TOKEN, "qualquer-valor");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    @DisplayName("Não aplica o filtro em rotas fora de /webhook/")
    void naoDeveFiltrarRotasForaDeWebhook() throws Exception {
        WebhookSecretFilter filtro = new WebhookSecretFilter("segredo-compartilhado");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/avisos");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }
}
