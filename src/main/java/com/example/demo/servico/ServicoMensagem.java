package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.util.WhatsappUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServicoMensagem {

    private static final long INTERVALO_GALERIA_MS = 2000;

    @Value("${wppconnect.url:http://wppconnect:21465}")
    private String urlBase = "http://wppconnect:21465";

    private RestTemplate restTemplate = criarRestTemplatePadrao();

    @Autowired
    private ServicoWppConnect wppConnectService;

    @Autowired
    private Observadorwhatsapp observador;

    @Autowired(required = false)
    private RegistroEnviosBot registroEnviosBot;

    public ServicoMensagem() {
    }

    @Autowired
    public ServicoMensagem(
            RestTemplate restTemplate,
            @Value("${wppconnect.url:http://wppconnect:21465}") String urlBase) {
        if (restTemplate != null) {
            this.restTemplate = restTemplate;
        }
        if (urlBase != null && !urlBase.isBlank()) {
            this.urlBase = urlBase;
        }
    }

    private static RestTemplate criarRestTemplatePadrao() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(5000);
        return new RestTemplate(fabrica);
    }

    public void enviarMensagemWPP(Empresa empresa, String numeroDestino, String texto) {
        String sessao = empresa.getSessaoWhatsapp();

        try {
            Map<String, Object> body = new HashMap<>();
            preencherDestino(body, numeroDestino);
            body.put("message", texto);
            registrarTexto(sessao, numeroDestino, texto);
            try {
                postWpp(sessao, "/send-message", body);
            } catch (RuntimeException falha) {
                esquecerTexto(sessao, numeroDestino, texto);
                throw falha;
            }
            observador.logSucesso(sessao, numeroDestino, "[RESPOSTA ENVIADA]: " + texto);
        } catch (Exception e) {
            observador.logErro(sessao, numeroDestino, "ServicoMensagem (Envio)", e);
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                wppConnectService.invalidarToken();
            }
        }
    }

    public boolean tentarEnviarMensagemWPP(Empresa empresa, String numeroDestino, String texto) {
        String sessao = empresa.getSessaoWhatsapp();
        try {
            Map<String, Object> body = new HashMap<>();
            preencherDestino(body, numeroDestino);
            body.put("message", texto);
            registrarTexto(sessao, numeroDestino, texto);
            try {
                postWpp(sessao, "/send-message", body);
            } catch (RuntimeException falha) {
                esquecerTexto(sessao, numeroDestino, texto);
                throw falha;
            }
            observador.logSucesso(sessao, numeroDestino, "[RESPOSTA ENVIADA]: " + texto);
            return true;
        } catch (Exception e) {
            observador.logErro(sessao, numeroDestino, "ServicoMensagem (Tentativa Envio)", e);
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                wppConnectService.invalidarToken();
            }
            return false;
        }
    }

    public void enviarImagemWPP(Empresa empresa, String numeroDestino, String urlImagem, String caption) {
        if (urlImagem == null || urlImagem.isBlank()) {
            return;
        }
        String sessao = empresa.getSessaoWhatsapp();
        String url = urlImagem.trim();

        try {
            Map<String, Object> body = new HashMap<>();
            preencherDestino(body, numeroDestino);
            body.put("path", url);
            body.put("filename", nomeArquivo(url));
            if (caption != null && !caption.isBlank()) {
                body.put("caption", caption);
            }
            if (registroEnviosBot != null) {
                registroEnviosBot.registrarMidia(sessao, numeroDestino);
                registroEnviosBot.registrarTexto(sessao, numeroDestino, caption);
            }
            try {
                postWpp(sessao, "/send-image", body);
            } catch (RuntimeException falha) {
                if (registroEnviosBot != null) {
                    registroEnviosBot.esquecerMidia(sessao, numeroDestino);
                    registroEnviosBot.esquecerTexto(sessao, numeroDestino, caption);
                }
                throw falha;
            }
            observador.logSucesso(sessao, numeroDestino, "[IMAGEM ENVIADA]: " + url);
        } catch (Exception e) {
            observador.logErro(sessao, numeroDestino, "ServicoMensagem (Imagem)", e);
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                wppConnectService.invalidarToken();
            }
        }
    }

    @Async
    public void enviarGaleriaComIntervalo(Empresa empresa, String numeroDestino, List<String> urls, Runnable aoTerminar) {
        List<String> galeria = urls == null ? List.of() : urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .limit(5)
                .toList();

        for (int i = 0; i < galeria.size(); i++) {
            enviarImagemWPP(empresa, numeroDestino, galeria.get(i), null);
            if (i < galeria.size() - 1) {
                try {
                    Thread.sleep(INTERVALO_GALERIA_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (aoTerminar != null) {
            aoTerminar.run();
        }
    }

    private void registrarTexto(String sessao, String numeroDestino, String texto) {
        if (registroEnviosBot != null) {
            registroEnviosBot.registrarTexto(sessao, numeroDestino, texto);
        }
    }

    private void esquecerTexto(String sessao, String numeroDestino, String texto) {
        if (registroEnviosBot != null) {
            registroEnviosBot.esquecerTexto(sessao, numeroDestino, texto);
        }
    }

    static void preencherDestino(Map<String, Object> body, String numeroDestino) {
        body.put("phone", WhatsappUtil.extrairApenasNumeros(numeroDestino));
        body.put("isGroup", false);
        body.put("isLid", WhatsappUtil.isLid(numeroDestino));
    }

    void postWpp(String sessao, String caminho, Map<String, Object> body) {
        String base = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        String url = base + "/api/" + sessao + caminho;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + wppConnectService.obterToken(sessao));

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    private String nomeArquivo(String url) {
        int barra = url.lastIndexOf('/');
        String nome = barra >= 0 ? url.substring(barra + 1) : url;
        int query = nome.indexOf('?');
        if (query >= 0) {
            nome = nome.substring(0, query);
        }
        return nome.isBlank() ? "foto.jpg" : nome;
    }
}
