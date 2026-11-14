package com.example.demo.servico;

import com.example.demo.dto.whatsapp.CodigoPareamentoResponseDTO;
import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import com.example.demo.util.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServicoWppConnect {

    private static final Logger log = LoggerFactory.getLogger(ServicoWppConnect.class);

    @Value("${wppconnect.url}")
    private String urlBase;

    @Value("${wppconnect.secret-key}")
    private String secretKey;

    @Value("${wppconnect.webhook-url:http://bot-java:8080/webhook/whatsapp}")
    private String webhookUrl;

    public record SaudeGateway(Situacao situacao, int conectadas, int aguardandoQr, List<String> desconectadas,
                                List<String> nomesConectadas) {

        public enum Situacao { OK, SEM_ROTA, INDISPONIVEL }

        static SaudeGateway indisponivel() {
            return new SaudeGateway(Situacao.INDISPONIVEL, 0, 0, List.of(), null);
        }

        static SaudeGateway semRota() {
            return new SaudeGateway(Situacao.SEM_ROTA, 0, 0, List.of(), null);
        }
    }

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
                log.info("Novo token gateway gerado para a sessão {}", sessao);
                return token;
            }
        } catch (Exception e) {
            log.warn("Erro ao obter token para a sessão {}: {}", sessao, e.getClass().getSimpleName());
        }
        return "";
    }

    public void invalidarToken() {
        tokensPorSessao.clear();
        log.info("Tokens gateway invalidados");
    }

        public void desconectar(String sessao) {
        if (sessao == null || sessao.isBlank()) {
            return;
        }
        invalidarToken(sessao);
        qrcodesPorSessao.remove(sessao);
        try {
            chamarWpp(sessao, "/close-session", HttpMethod.POST, null, Map.class);
        } catch (Exception e) {
            log.warn("Falha ao fechar sessao gateway {}: {}", sessao, e.getClass().getSimpleName());
        }
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
        log.info("QR Code em cache para a sessão {}", sessao);
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
            SessaoStatusResponseDTO atual = consultarStatus(sessao);
            if ("CONNECTED".equals(atual.status())) {
                return atual;
            }
            return dispararStartSession(sessao);
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidarToken(sessao);
            try {
                return dispararStartSession(sessao);
            } catch (Exception retry) {
                log.warn("Falha ao iniciar sessão gateway {}: {}", sessao, retry.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.warn("Falha ao iniciar sessão gateway {}: {}", sessao, e.getClass().getSimpleName());
        }
        return consultarStatus(sessao);
    }

    @SuppressWarnings("unchecked")
    private SessaoStatusResponseDTO dispararStartSession(String sessao) {
        Map<String, Object> body = new HashMap<>();
        body.put("webhook", webhookUrl);
        body.put("waitQrCode", false);
        Map<String, Object> corpo = chamarWpp(sessao, "/start-session", HttpMethod.POST, body, Map.class);
        SessaoStatusResponseDTO dto = paraDto(sessao, corpo);
        if ("CONNECTED".equals(dto.status()) || "INITIALIZING".equals(dto.status()) || dto.qrcodeBase64() != null) {
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
        boolean semQr = qrcode == null || qrcode.isBlank();
        if (valor.contains("INITIALIZING") && semQr) {
            return "INITIALIZING";
        }
        if (valor.contains("QR") || valor.contains("INIT") || !semQr) {
            return "QRCODE";
        }
        return "DISCONNECTED";
    }

    @SuppressWarnings("unchecked")
    public SaudeGateway consultarSaude() {
        try {
            HttpHeaders cabecalhos = new HttpHeaders();
            cabecalhos.setBearerAuth(secretKey);
            Map<String, Object> corpo = restTemplate.exchange(
                    urlBase + "/api/health", HttpMethod.GET, new HttpEntity<>(cabecalhos), Map.class).getBody();
            if (corpo == null || !(corpo.get("sessoes") instanceof Map<?, ?> sessoes)) {
                return SaudeGateway.indisponivel();
            }
            return new SaudeGateway(
                    SaudeGateway.Situacao.OK,
                    inteiro(sessoes.get("conectadas")),
                    inteiro(sessoes.get("aguardandoQr")),
                    nomes(sessoes.get("desconectadas")),
                    sessoes.get("nomesConectadas") instanceof List<?> ? nomes(sessoes.get("nomesConectadas")) : null);
        } catch (HttpClientErrorException.NotFound e) {
            return SaudeGateway.semRota();
        } catch (Exception e) {
            log.warn("Gateway WhatsApp sem resposta na rota de saúde: {}", e.getClass().getSimpleName());
            return SaudeGateway.indisponivel();
        }
    }

    private int inteiro(Object valor) {
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private List<String> nomes(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        return lista.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    public CodigoPareamentoResponseDTO solicitarCodigoPareamento(String sessao, String telefone) {
        try {
            return pedirCodigo(sessao, telefone);
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidarToken(sessao);
            try {
                return pedirCodigo(sessao, telefone);
            } catch (Exception retry) {
                throw traduzirFalhaDoCodigo(sessao, telefone, retry);
            }
        } catch (Exception e) {
            throw traduzirFalhaDoCodigo(sessao, telefone, e);
        }
    }

    @SuppressWarnings("unchecked")
    private CodigoPareamentoResponseDTO pedirCodigo(String sessao, String telefone) {
        Map<String, Object> corpo = chamarWpp(sessao, "/request-pairing-code", HttpMethod.POST,
                Map.of("phone", telefone), Map.class);
        if (corpo != null && corpo.get("code") instanceof String codigo && !codigo.isBlank()) {
            log.info("Código de pareamento gerado para a sessão {} (telefone {})", sessao, MascaraLog.numero(telefone));
            return new CodigoPareamentoResponseDTO(sessao, codigo);
        }
        throw new IllegalStateException("resposta sem código");
    }

    private ResponseStatusException traduzirFalhaDoCodigo(String sessao, String telefone, Exception e) {
        log.warn("Código de pareamento não gerado para a sessão {} (telefone {}): {}",
                sessao, MascaraLog.numero(telefone), e.getClass().getSimpleName());
        if (e instanceof HttpClientErrorException http) {
            int codigo = http.getStatusCode().value();
            if (codigo == HttpStatus.BAD_REQUEST.value()) {
                return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone inválido");
            }
            if (codigo == HttpStatus.CONFLICT.value()) {
                return new ResponseStatusException(HttpStatus.CONFLICT, "Sessão não está aguardando pareamento");
            }
            if (codigo == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Limite de pedidos de código atingido; tente de novo em alguns minutos");
            }
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "pareamento por código indisponível");
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
        if ("INITIALIZING".equals(dto.status())) {
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
        log.warn("Falha ao consultar status gateway {}: {}", sessao, e.getClass().getSimpleName());
        return new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null);
    }
}
