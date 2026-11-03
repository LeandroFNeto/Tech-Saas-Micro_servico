package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServicoIA {

    private static final Logger log = LoggerFactory.getLogger(ServicoIA.class);

    private final String geminiApiUrl;
    private final String geminiApiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ServicoIA(
            RestTemplate restTemplate,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent}") String geminiApiUrl,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.timeout-ms:3000}") int timeoutMs) {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
            fabrica.setConnectTimeout(timeoutMs);
            fabrica.setReadTimeout(timeoutMs);
            this.restTemplate = new RestTemplate(fabrica);
        } else {
            this.restTemplate = restTemplate;
        }
        this.geminiApiUrl = geminiApiUrl;
        this.geminiApiKey = geminiApiKey;
        this.objectMapper = new ObjectMapper();
    }

    public Optional<String> responderAtendimento(Empresa empresa, String mensagemUsuario) {
        if (empresa == null || !Boolean.TRUE.equals(empresa.getUsaIA())) {
            return Optional.empty();
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return Optional.empty();
        }
        if (mensagemUsuario == null || mensagemUsuario.isBlank()) {
            return Optional.empty();
        }

        String textoNormalizado = mensagemUsuario.trim().toLowerCase();
        if (ehComandoOuOpcao(textoNormalizado)) {
            return Optional.empty();
        }

        String prompt = construirPromptEmpresa(empresa, mensagemUsuario);
        String resposta = gerarRespostaHumanizada(prompt);

        if (resposta == null || resposta.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(resposta);
    }

    private boolean ehComandoOuOpcao(String texto) {
        if (texto.matches("^[0-9]+$")) {
            return true;
        }
        return texto.equals("menu") || texto.equals("voltar") || texto.equals("sair") ||
                texto.equals("cancelar") || texto.equals("sim") || texto.equals("nao") || texto.equals("não");
    }

    public String construirPromptEmpresa(Empresa empresa, String mensagemUsuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é o assistente virtual da empresa '").append(empresa.getNome()).append("'.\n");
        sb.append("Ramo: ").append(empresa.getRamoDeAtuacao()).append(".\n");
        if (empresa.getTabelaDePrecos() != null && !empresa.getTabelaDePrecos().isBlank()) {
            sb.append("Preços: ").append(empresa.getTabelaDePrecos()).append(".\n");
        }
        if (empresa.getRegrasLocacao() != null && !empresa.getRegrasLocacao().isBlank()) {
            sb.append("Regras: ").append(empresa.getRegrasLocacao()).append(".\n");
        }
        if (empresa.getLinkGoogleMaps() != null && !empresa.getLinkGoogleMaps().isBlank()) {
            sb.append("Localização: ").append(empresa.getLinkGoogleMaps()).append(".\n");
        }
        sb.append("Instruções:\n");
        sb.append("1. Responda em português do Brasil, de forma humana, simpática e concisa (máximo 2 a 3 frases curtas).\n");
        sb.append("2. Se o cliente apenas saudou, cumprimente-o calorosamente pelo nome da empresa e convide-o a escolher uma opção do menu.\n");
        sb.append("3. Se o cliente fez pergunta sobre regras ou valores, responda exclusivamente com base nos dados fornecidos.\n");
        sb.append("4. Finalize lembrando cordialmente que ele pode digitar 1 para ver a agenda, 2 para pesquisar data ou 3 para solicitar reserva.\n");
        sb.append("5. Mensagem do cliente: \"").append(mensagemUsuario.replace("\"", "'")).append("\"");
        return sb.toString();
    }

    public String gerarRespostaHumanizada(String prompt) {
        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> payload = Map.of("contents", List.of(content));
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            return extrairTextoDaResposta(response.getBody());

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Cota gratuita da IA atingida (429)");
            return null;
        } catch (Exception e) {
            log.warn("Erro ao comunicar com a IA: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private String extrairTextoDaResposta(String jsonResposta) {
        if (jsonResposta == null || jsonResposta.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonResposta);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.isNull()) {
                return null;
            }
            String texto = textNode.asText().trim();
            return texto.isEmpty() ? null : texto;
        } catch (Exception e) {
            log.warn("Erro ao fazer parse da resposta da IA: {}", e.getMessage());
            return null;
        }
    }
}