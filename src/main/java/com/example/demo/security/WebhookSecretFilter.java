package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WebhookSecretFilter extends OncePerRequestFilter {

    public static final String HEADER_WEBHOOK_TOKEN = "x-webhook-token";

    private static final Logger log = LoggerFactory.getLogger(WebhookSecretFilter.class);

    private final String webhookSecret;

    public WebhookSecretFilter(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public static boolean segredoValido(String esperado, String recebido) {
        if (esperado == null || esperado.isBlank() || recebido == null || recebido.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recebido.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/webhook/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String recebido = request.getHeader(HEADER_WEBHOOK_TOKEN);
        if (!segredoValido(webhookSecret, recebido)) {
            log.warn("Webhook WhatsApp recusado em {}: cabeçalho {} ausente ou inválido",
                    request.getRequestURI(), HEADER_WEBHOOK_TOKEN);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
