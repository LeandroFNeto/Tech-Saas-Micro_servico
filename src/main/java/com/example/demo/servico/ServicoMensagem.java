package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.util.WhatsappUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServicoMensagem {

    private static final long INTERVALO_GALERIA_MS = 2000;

    @Autowired
    private ServicoWppConnect wppConnectService;

    @Autowired
    private Observadorwhatsapp observador;

    @Autowired(required = false)
    private RegistroEnviosBot registroEnviosBot;

    public void enviarMensagemWPP(Empresa empresa, String numeroDestino, String texto) {
        String sessao = empresa.getSessaoWhatsapp();

        try {
            Map<String, Object> body = new HashMap<>();
            preencherDestino(body, numeroDestino);
            body.put("message", texto);
            registrarTexto(numeroDestino, texto);
            try {
                postWpp(sessao, "/send-message", body);
            } catch (RuntimeException falha) {
                esquecerTexto(numeroDestino, texto);
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
            registrarTexto(numeroDestino, texto);
            try {
                postWpp(sessao, "/send-message", body);
            } catch (RuntimeException falha) {
                esquecerTexto(numeroDestino, texto);
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
                registroEnviosBot.registrarMidia(numeroDestino);
                registroEnviosBot.registrarTexto(numeroDestino, caption);
            }
            try {
                postWpp(sessao, "/send-image", body);
            } catch (RuntimeException falha) {
                if (registroEnviosBot != null) {
                    registroEnviosBot.esquecerMidia(numeroDestino);
                    registroEnviosBot.esquecerTexto(numeroDestino, caption);
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

    private void registrarTexto(String numeroDestino, String texto) {
        if (registroEnviosBot != null) {
            registroEnviosBot.registrarTexto(numeroDestino, texto);
        }
    }

    private void esquecerTexto(String numeroDestino, String texto) {
        if (registroEnviosBot != null) {
            registroEnviosBot.esquecerTexto(numeroDestino, texto);
        }
    }

    static void preencherDestino(Map<String, Object> body, String numeroDestino) {
        body.put("phone", WhatsappUtil.extrairApenasNumeros(numeroDestino));
        body.put("isGroup", false);
        body.put("isLid", WhatsappUtil.isLid(numeroDestino));
    }

    void postWpp(String sessao, String caminho, Map<String, Object> body) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://wppconnect:21465/api/" + sessao + caminho;

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
