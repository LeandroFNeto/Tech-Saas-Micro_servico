package com.example.demo.servico;

import com.example.demo.model.Empresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoIATest {

    @Mock
    private RestTemplate restTemplate;

    private ServicoIA servicoIA;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        servicoIA = new ServicoIA(restTemplate, "https://api.fake.gemini/generate", "fake-key-123", 3000);
        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Recanto das Palmeiras");
        empresa.setRamoDeAtuacao("LOCACAO");
        empresa.setTabelaDePrecos("Diária R$ 500,00");
        empresa.setRegrasLocacao("Proibido som alto após as 22h");
        empresa.setLinkGoogleMaps("https://maps.google.com/?q=-23.5,-46.6");
        empresa.setGoogleCalendarId("segredo-agenda@gmail.com");
        empresa.setUsaIA(true);
    }

    @Test
    @DisplayName("Constrói prompt estrito com dados da empresa sem expor calendário")
    void deveConstruirPromptComDadosExclusivosDaEmpresaSemVazarSegredos() {
        String prompt = servicoIA.construirPromptEmpresa(empresa, "Bom dia, quanto custa a diária?");

        assertThat(prompt).contains("Recanto das Palmeiras");
        assertThat(prompt).contains("Diária R$ 500,00");
        assertThat(prompt).contains("Proibido som alto");
        assertThat(prompt).contains("https://maps.google.com");
        assertThat(prompt).doesNotContain("segredo-agenda@gmail.com");
    }

    @Test
    @DisplayName("Retorna empty quando empresa não usa IA")
    void deveRetornarEmptyQuandoEmpresaNaoUsaIA() {
        empresa.setUsaIA(false);

        Optional<String> resposta = servicoIA.responderAtendimento(empresa, "Olá, bom dia");

        assertThat(resposta).isEmpty();
    }

    @Test
    @DisplayName("Retorna empty para comandos e opções de menu poupando cota da IA")
    void deveRetornarEmptyParaComandosDeMenu() {
        assertThat(servicoIA.responderAtendimento(empresa, "1")).isEmpty();
        assertThat(servicoIA.responderAtendimento(empresa, "2")).isEmpty();
        assertThat(servicoIA.responderAtendimento(empresa, "0")).isEmpty();
        assertThat(servicoIA.responderAtendimento(empresa, "menu")).isEmpty();
        assertThat(servicoIA.responderAtendimento(empresa, "voltar")).isEmpty();
    }

    @Test
    @DisplayName("Retorna resposta humanizada com sucesso quando API Gemini responder")
    void deveRetornarRespostaHumanizadaComSucesso() {
        String jsonResposta = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Olá! Bem-vindo ao Recanto das Palmeiras. Nossa diária custa R$ 500."
                      }
                    ]
                  }
                }
              ]
            }
            """;

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResposta, HttpStatus.OK));

        Optional<String> resposta = servicoIA.responderAtendimento(empresa, "Boa tarde, qual o valor?");

        assertThat(resposta).isPresent();
        assertThat(resposta.get()).contains("Recanto das Palmeiras");
    }

    @Test
    @DisplayName("Decodifica corretamente resposta com aspas e caracteres especiais sem truncar")
    void deveDecodificarCorretamenteRespostaComAspas() {
        String jsonResposta = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Olá! O espaço \\"Recanto das Palmeiras\\" está pronto para recebê-lo."
                      }
                    ]
                  }
                }
              ]
            }
            """;

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResposta, HttpStatus.OK));

        Optional<String> resposta = servicoIA.responderAtendimento(empresa, "Olá");

        assertThat(resposta).isPresent();
        assertThat(resposta.get()).isEqualTo("Olá! O espaço \"Recanto das Palmeiras\" está pronto para recebê-lo.");
    }

    @Test
    @DisplayName("Aciona fallback silencioso e retorna empty em caso de erro 429 de cota excedida")
    void deveAcionarFallbackSilenciosoEmErro429() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Rate limit", HttpHeaders.EMPTY, null, null));

        Optional<String> resposta = servicoIA.responderAtendimento(empresa, "Olá, bom dia!");

        assertThat(resposta).isEmpty();
    }

    @Test
    @DisplayName("Aciona fallback silencioso e retorna empty em caso de timeout de rede")
    void deveAcionarFallbackSilenciosoEmTimeout() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Read timeout"));

        Optional<String> resposta = servicoIA.responderAtendimento(empresa, "Olá!");

        assertThat(resposta).isEmpty();
    }
}