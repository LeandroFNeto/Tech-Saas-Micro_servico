package com.example.demo.servico;

import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServicoWppConnect {

    @Value("${wppconnect.url}")
    private String urlBase;

    @Value("${wppconnect.secret-key}")
    private String secretKey;

    @Value("${wppconnect.webhook-url:http://bot-java:8080/webhook/whatsapp}")
    private String webhookUrl;

    private final RestTemplate restTemplate;
    private final Map<String, String> tokensPorSessao = new ConcurrentHashMap<>();

    public ServicoWppConnect(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String obterToken(String sessao) {
        String cache = tokensPorSessao.get(sessao);
        if (cache != null) {
            return cache;
        }

        try {
            String url = urlBase + "/api/" + sessao + "/" + secretKey + "/generate-token";
            @SuppressWarnings("unchecked")
            Map<String, Object> resposta = restTemplate.postForObject(url, null, Map.class);

            if (resposta != null && resposta.get("token") instanceof String token) {
                tokensPorSessao.put(sessao, token);
                System.out.println("✅ Novo Token WPPConnect gerado com sucesso para a sessão: " + sessao);
                return token;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Erro ao obter token para a sessão " + sessao + ": " + e.getMessage());
        }
        return "";
    }

    public void invalidarToken() {
        tokensPorSessao.clear();
        System.out.println("🔄 Token WPPConnect invalidado. Um novo será gerado na próxima requisição.");
    }

    public void invalidarToken(String sessao) {
        tokensPorSessao.remove(sessao);
    }

    public SessaoStatusResponseDTO consultarStatus(String sessao) {
        try {
            return consultarStatusNoWpp(sessao);
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidarToken(sessao);
            try {
                return consultarStatusNoWpp(sessao);
            } catch (Exception retry) {
                return desconectado(sessao, retry);
            }
        } catch (Exception e) {
            return desconectado(sessao, e);
        }
    }

    public SessaoStatusResponseDTO iniciarSessao(String sessao) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("webhook", webhookUrl);
            body.put("waitQrCode", false);
            chamarWpp(sessao, "/start-session", HttpMethod.POST, body, String.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidarToken(sessao);
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("webhook", webhookUrl);
                body.put("waitQrCode", false);
                chamarWpp(sessao, "/start-session", HttpMethod.POST, body, String.class);
            } catch (Exception retry) {
                System.out.println("⚠️ Falha ao iniciar sessão WPPConnect (" + sessao + "): " + retry.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Falha ao iniciar sessão WPPConnect (" + sessao + "): " + e.getMessage());
        }
        return consultarStatus(sessao);
    }

    SessaoStatusResponseDTO paraDto(String sessao, Map<String, Object> corpo) {
        if (corpo == null) {
            return new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null);
        }

        String bruto = String.valueOf(corpo.getOrDefault("status", ""));
        String qrcode = extrairQrcode(corpo);
        String status = normalizarStatus(bruto, qrcode);
        return new SessaoStatusResponseDTO(sessao, status, "QRCODE".equals(status) ? qrcode : null);
    }

    String normalizarStatus(String bruto, String qrcode) {
        String valor = bruto == null ? "" : bruto.trim().toUpperCase(Locale.ROOT);
        if ((valor.contains("CONNECT") && !valor.contains("DISCONNECT"))
                || valor.contains("INCHAT")
                || valor.contains("ISLOGGED")) {
            return "CONNECTED";
        }
        if (valor.contains("QR") || valor.contains("INIT")
                || (qrcode != null && !qrcode.isBlank())) {
            return "QRCODE";
        }
        return "DISCONNECTED";
    }

    private SessaoStatusResponseDTO consultarStatusNoWpp(String sessao) {
        @SuppressWarnings("unchecked")
        Map<String, Object> corpo = chamarWpp(sessao, "/status-session", HttpMethod.GET, null, Map.class);
        return paraDto(sessao, corpo);
    }

    private String extrairQrcode(Map<String, Object> corpo) {
        Object qrcode = corpo.get("qrcode");
        if (!(qrcode instanceof String texto) || texto.isBlank() || "null".equalsIgnoreCase(texto)) {
            return null;
        }
        return texto;
    }

    private <T> T chamarWpp(String sessao, String caminho, HttpMethod metodo, Object body, Class<T> tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(obterToken(sessao));
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        ResponseEntity<T> resposta = restTemplate.exchange(
                urlBase + "/api/" + sessao + caminho,
                metodo,
                request,
                tipo
        );
        return resposta.getBody();
    }

    private SessaoStatusResponseDTO desconectado(String sessao, Exception e) {
        System.out.println("⚠️ Falha ao consultar status WPPConnect (" + sessao + "): " + e.getMessage());
        return new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null);
    }
}
