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

import java.util.Base64;
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
    private final Map<String, String> qrcodesPorSessao = new ConcurrentHashMap<>();

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

    public void guardarQrcode(String sessao, String qrcode) {
        String normalizado = normalizarQrcode(qrcode);
        if (sessao == null || sessao.isBlank() || normalizado == null) {
            return;
        }
        qrcodesPorSessao.put(sessao, normalizado);
        System.out.println("✅ QR Code em cache para a sessão: " + sessao);
    }

    public SessaoStatusResponseDTO consultarStatus(String sessao) {
        try {
            return completarComQr(sessao, consultarStatusNoWpp(sessao));
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidarToken(sessao);
            try {
                return completarComQr(sessao, consultarStatusNoWpp(sessao));
            } catch (Exception retry) {
                return desconectado(sessao, retry);
            }
        } catch (Exception e) {
            return desconectado(sessao, e);
        }
    }

    public SessaoStatusResponseDTO iniciarSessao(String sessao) {
        try {
            return dispararStartSession(sessao);
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidarToken(sessao);
            try {
                return dispararStartSession(sessao);
            } catch (Exception retry) {
                System.out.println("⚠️ Falha ao iniciar sessão WPPConnect (" + sessao + "): " + retry.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Falha ao iniciar sessão WPPConnect (" + sessao + "): " + e.getMessage());
        }
        return consultarStatus(sessao);
    }

    @SuppressWarnings("unchecked")
    private SessaoStatusResponseDTO dispararStartSession(String sessao) {
        Map<String, Object> body = new HashMap<>();
        body.put("webhook", webhookUrl);
        // false: o Chromium abre em segundo plano. true faz o Java esperar o QR
        // e, se a sessão antiga auto-fechar, a requisição fica presa até o timeout.
        body.put("waitQrCode", false);
        Map<String, Object> corpo = chamarWpp(sessao, "/start-session", HttpMethod.POST, body, Map.class);
        SessaoStatusResponseDTO dto = paraDto(sessao, corpo);
        if ("CONNECTED".equals(dto.status()) || dto.qrcodeBase64() != null) {
            return dto;
        }
        return new SessaoStatusResponseDTO(sessao, "QRCODE", qrcodesPorSessao.get(sessao));
    }

    SessaoStatusResponseDTO paraDto(String sessao, Map<String, Object> corpo) {
        if (corpo == null) {
            return new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null);
        }

        String bruto = String.valueOf(corpo.getOrDefault("status", ""));
        String qrcode = extrairQrcode(corpo);
        guardarQrcode(sessao, qrcode);
        String status = normalizarStatus(bruto, qrcode);
        return new SessaoStatusResponseDTO(sessao, status, "QRCODE".equals(status) ? primeiroQr(sessao, qrcode) : null);
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
        if (qrcode instanceof String texto) {
            return normalizarQrcode(texto);
        }
        return null;
    }

    private SessaoStatusResponseDTO completarComQr(String sessao, SessaoStatusResponseDTO dto) {
        if ("CONNECTED".equals(dto.status())) {
            qrcodesPorSessao.remove(sessao);
            return dto;
        }
        if (dto.qrcodeBase64() != null && !dto.qrcodeBase64().isBlank()) {
            guardarQrcode(sessao, dto.qrcodeBase64());
            return dto;
        }
        String cache = qrcodesPorSessao.get(sessao);
        if (cache != null) {
            return new SessaoStatusResponseDTO(sessao, "QRCODE", cache);
        }
        String png = buscarQrcodePng(sessao);
        if (png != null) {
            guardarQrcode(sessao, png);
            return new SessaoStatusResponseDTO(sessao, "QRCODE", png);
        }
        return dto;
    }

    private String primeiroQr(String sessao, String qrcode) {
        if (qrcode != null) {
            return qrcode;
        }
        return qrcodesPorSessao.get(sessao);
    }

    private String normalizarQrcode(String qrcode) {
        if (qrcode == null || qrcode.isBlank() || "null".equalsIgnoreCase(qrcode)) {
            return null;
        }
        return qrcode.startsWith("data:") ? qrcode : "data:image/png;base64," + qrcode;
    }

    private String buscarQrcodePng(String sessao) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(obterToken(sessao));
            headers.setAccept(java.util.List.of(MediaType.IMAGE_PNG, MediaType.ALL));
            ResponseEntity<byte[]> resposta = restTemplate.exchange(
                    urlBase + "/api/" + sessao + "/qrcode-session",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            MediaType tipo = resposta.getHeaders().getContentType();
            if (tipo != null && MediaType.APPLICATION_JSON.isCompatibleWith(tipo)) {
                return null;
            }
            byte[] corpo = resposta.getBody();
            if (corpo == null || corpo.length < 32) {
                return null;
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(corpo);
        } catch (Exception e) {
            return null;
        }
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
