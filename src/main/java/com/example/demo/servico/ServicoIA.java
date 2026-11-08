package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServicoIA {

    private static final Logger log = LoggerFactory.getLogger(ServicoIA.class);

    private static final String ORIENTACAO_BASICA =
            "Você faz parte da equipe da empresa e atende um cliente pelo WhatsApp. Converse como uma pessoa, em português do Brasil, "
                    + "respondendo primeiro o que foi perguntado. Use só os dados cadastrados, nunca confirme reserva e, "
                    + "sem a informação, diga que um atendente vai responder.";

    public static final String MENSAGEM_SEM_MODELO =
            "Não consegui te responder direito agora. Digite *menu* para ver as opções ou o número de uma delas.";
    public static final String CONVITE_MENU = " Como posso te ajudar? Se quiser ver as opções, é só digitar *menu*.";

    private final String geminiApiUrl;
    private final String geminiApiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConversaCurta conversaCurta = new ConversaCurta();
    private volatile String orientacaoAtendimento;
    private GuardaCotaIA guardaCota;
    private CacheRespostaIA cacheResposta;

    public ServicoIA(
            RestTemplate restTemplate,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent}") String geminiApiUrl,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.timeout-ms:3000}") int timeoutMs) {
        if (restTemplate == null
                || (restTemplate.getClass() == RestTemplate.class
                && restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory)) {
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

    @Autowired(required = false)
    void setGuardaCota(GuardaCotaIA guardaCota) {
        this.guardaCota = guardaCota;
    }

    @Autowired(required = false)
    void setCacheResposta(CacheRespostaIA cacheResposta) {
        this.cacheResposta = cacheResposta;
    }

    public Optional<String> conversar(Empresa empresa, String contato, String mensagemUsuario, String menuDaEmpresa) {
        if (empresa == null || !Boolean.TRUE.equals(empresa.getUsaIA())) {
            return Optional.empty();
        }
        if (mensagemUsuario == null || mensagemUsuario.isBlank()) {
            return Optional.empty();
        }
        if (ehComandoOuOpcao(mensagemUsuario.trim().toLowerCase())) {
            return Optional.empty();
        }
        Optional<ConversaCurta.Resposta> curta = conversaCurta.responder(contato, mensagemUsuario);
        if (curta.isPresent()) {
            ConversaCurta.Resposta resposta = curta.get();
            boolean comConvite = resposta.tipo() == ConversaCurta.Tipo.CUMPRIMENTO
                    || resposta.tipo() == ConversaCurta.Tipo.BEM_ESTAR;
            return Optional.of(comConvite ? resposta.texto() + CONVITE_MENU : resposta.texto());
        }
        return responderAtendimento(empresa, mensagemUsuario, menuDaEmpresa);
    }

    public Optional<String> responderAtendimento(Empresa empresa, String mensagemUsuario) {
        return responderAtendimento(empresa, mensagemUsuario, null);
    }

    public Optional<String> responderAtendimento(Empresa empresa, String mensagemUsuario, String menuDaEmpresa) {
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

        String prompt = construirPromptEmpresa(empresa, mensagemUsuario, menuDaEmpresa);
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
        return construirPromptEmpresa(empresa, mensagemUsuario, null);
    }

    public String construirPromptEmpresa(Empresa empresa, String mensagemUsuario, String menuDaEmpresa) {
        StringBuilder sb = new StringBuilder();
        sb.append(orientacaoAtendimento()).append("\n\n");
        sb.append("Empresa: '").append(empresa.getNome()).append("'.\n");
        sb.append("Ramo: ").append(empresa.getRamoDeAtuacao()).append(".\n");
        sb.append(empresa.isLocacaoPorHora()
                ? "Modalidade: locação cobrada por hora.\n"
                : "Modalidade: locação cobrada por diária.\n");
        if (empresa.getTabelaDePrecos() != null && !empresa.getTabelaDePrecos().isBlank()) {
            sb.append("Preços: ").append(empresa.getTabelaDePrecos()).append(".\n");
        } else {
            sb.append("Preços: não cadastrados; não informe valor, diga que um atendente responde.\n");
        }
        if (empresa.getRegrasLocacao() != null && !empresa.getRegrasLocacao().isBlank()) {
            sb.append("Regras: ").append(empresa.getRegrasLocacao()).append(".\n");
        }
        if (empresa.getLinkGoogleMaps() != null && !empresa.getLinkGoogleMaps().isBlank()) {
            sb.append("Localização: ").append(empresa.getLinkGoogleMaps()).append(".\n");
        }
        if (menuDaEmpresa != null && !menuDaEmpresa.isBlank()) {
            sb.append("\nOpções disponíveis no menu desta empresa (o cliente digita o número):\n")
                    .append(menuDaEmpresa.trim()).append("\n");
        } else {
            sb.append("\nO cliente pode digitar 'menu' para ver as opções.\n");
        }
        sb.append("\nMensagem do cliente: \"").append(mensagemUsuario.replace("\"", "'")).append("\"");
        return sb.toString();
    }

    private String orientacaoAtendimento() {
        String atual = orientacaoAtendimento;
        if (atual != null) {
            return atual;
        }
        atual = carregarOrientacaoDoRecurso("assistente/atendimento-empresa.md");
        orientacaoAtendimento = atual;
        return atual;
    }

    String carregarOrientacaoDoRecurso(String caminhoRecurso) {
        try (InputStream entrada = new ClassPathResource(caminhoRecurso).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.warn("Orientação de atendimento indisponível; usando instruções básicas");
            return ORIENTACAO_BASICA;
        }
    }

    public String gerarRespostaHumanizada(String prompt) {
        if (cacheResposta != null) {
            Optional<String> guardada = cacheResposta.obter(prompt);
            if (guardada.isPresent()) {
                return guardada.get();
            }
        }
        if (guardaCota != null && !guardaCota.reservar()) {
            log.info("Chamada a IA suspensa pela guarda de cota");
            return null;
        }
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

            registrarConsumo(response.getBody());
            String texto = extrairTextoDaResposta(response.getBody());
            if (texto != null && cacheResposta != null) {
                cacheResposta.guardar(prompt, texto);
            }
            return texto;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Cota da IA atingida (429)");
            if (guardaCota != null) {
                guardaCota.registrarCotaEsgotada();
            }
            return null;
        } catch (Exception e) {
            log.warn("Erro ao comunicar com a IA: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private void registrarConsumo(String jsonResposta) {
        if (guardaCota == null) {
            return;
        }
        Integer entrada = null;
        Integer saida = null;
        try {
            JsonNode uso = objectMapper.readTree(jsonResposta == null ? "{}" : jsonResposta).path("usageMetadata");
            if (uso.has("promptTokenCount") && uso.has("candidatesTokenCount")) {
                entrada = uso.path("promptTokenCount").asInt();
                saida = uso.path("candidatesTokenCount").asInt() + uso.path("thoughtsTokenCount").asInt(0);
            }
        } catch (Exception e) {
            entrada = null;
            saida = null;
        }
        guardaCota.registrarUso(entrada, saida);
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
            log.warn("Erro ao fazer parse da resposta da IA: {}", e.getClass().getSimpleName());
            return null;
        }
    }
}
