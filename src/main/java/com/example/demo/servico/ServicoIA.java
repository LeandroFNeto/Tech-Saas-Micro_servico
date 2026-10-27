package com.example.demo.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ServicoIA {

    private static final Logger log = LoggerFactory.getLogger(ServicoIA.class);

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate;

    public ServicoIA(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String gerarRespostaHumanizada(String prompt) {
        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\":[{\"text\": \"" + prompt.replace("\"", "\\\"").replace("\n", " ") + "\"}]\n" +
                    "    }]\n" +
                    "}";

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            return extrairTextoDaResposta(response.getBody());

        } catch (Exception e) {
            log.warn("Erro ao comunicar com a IA: {}", e.getClass().getSimpleName());
            return "Desculpe, nosso assistente inteligente está indisponível no momento. " +
                    "Por favor, aguarde um instante ou tente novamente.";
        }
    }

    private String extrairTextoDaResposta(String jsonResposta) {
        if (jsonResposta == null || !jsonResposta.contains("\"text\":")) {
            return "Erro ao processar a resposta da IA.";
        }

        String[] partes = jsonResposta.split("\"text\": \"");
        if (partes.length > 1) {
            String textoBruto = partes[1].split("\"")[0];
            return textoBruto.replace("\\n", "\n").trim();
        }
        return "Não consegui formular uma resposta agora.";
    }
}