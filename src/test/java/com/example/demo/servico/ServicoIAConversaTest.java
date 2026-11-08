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
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoIAConversaTest {

    private static final String RESPOSTA_JSON = """
            {"candidates":[{"content":{"parts":[{"text":"Claro, posso ajudar com isso."}]}}],
             "usageMetadata":{"promptTokenCount":800,"candidatesTokenCount":100,"thoughtsTokenCount":50}}
            """;

    @Mock
    private RestTemplate restTemplate;

    private ServicoIA servicoIA;
    private GuardaCotaIA guarda;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        servicoIA = new ServicoIA(restTemplate, "https://api.fake.gemini/generate", "fake-key", 3000);
        guarda = new GuardaCotaIA(100, 100, 100, 0.30, 2.50, 5.50, 0.01, 300);
        servicoIA.setGuardaCota(guarda);
        servicoIA.setCacheResposta(new CacheRespostaIA(60));
        empresa = new Empresa();
        empresa.setNome("Recanto das Palmeiras");
        empresa.setRamoDeAtuacao("LOCACAO");
        empresa.setUsaIA(true);
    }

    @Test
    @DisplayName("Cumprimento é respondido sem chamar o modelo")
    void cumprimentoNaoChamaModelo() {
        Optional<String> resposta = servicoIA.conversar(empresa, "5511", "bom dia", "1 - Fotos");

        assertThat(resposta).isPresent();
        assertThat(resposta.get()).startsWith("Bom dia").contains("*menu*");
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("Agradecimento não leva o convite ao menu")
    void agradecimentoSemConvite() {
        Optional<String> resposta = servicoIA.conversar(empresa, "5511", "obrigado", "1 - Fotos");

        assertThat(resposta).isPresent();
        assertThat(resposta.get()).doesNotContain("*menu*");
    }

    @Test
    @DisplayName("Empresa sem IA e opção numérica não entram na conversa livre")
    void semIaOuOpcaoNaoConversa() {
        assertThat(servicoIA.conversar(empresa, "5511", "2", "menu")).isEmpty();
        empresa.setUsaIA(false);
        assertThat(servicoIA.conversar(empresa, "5511", "oi", "menu")).isEmpty();
    }

    @Test
    @DisplayName("Texto livre vai ao modelo e a mesma pergunta usa o cache")
    void textoLivreUsaModeloUmaVez() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(RESPOSTA_JSON, HttpStatus.OK));

        Optional<String> primeira = servicoIA.conversar(empresa, "5511", "vocês têm espaço para festa?", "1 - Fotos");
        Optional<String> segunda = servicoIA.conversar(empresa, "5522", "vocês têm espaço para festa?", "1 - Fotos");

        assertThat(primeira).contains("Claro, posso ajudar com isso.");
        assertThat(segunda).contains("Claro, posso ajudar com isso.");
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        assertThat(guarda.gastoDoDiaReais()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Erro 429 pausa o modelo e as próximas chamadas não saem")
    void erro429PausaModelo() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "limite", HttpHeaders.EMPTY, null, null));

        assertThat(servicoIA.conversar(empresa, "5511", "qual o horário de vocês?", "menu")).isEmpty();
        assertThat(servicoIA.conversar(empresa, "5511", "tem estacionamento no local?", "menu")).isEmpty();

        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("No teto de gasto o modelo não é chamado")
    void tetoDeGastoBloqueiaModelo() {
        GuardaCotaIA estourada = new GuardaCotaIA(100, 100, 0.0, 0.30, 2.50, 5.50, 0.01, 300);
        servicoIA.setGuardaCota(estourada);

        assertThat(servicoIA.conversar(empresa, "5511", "qual o horário de vocês?", "menu")).isEmpty();
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("A orientação mantém a liberdade de conversa e as proibições")
    void orientacaoMantemProibicoes() {
        String orientacao = servicoIA.carregarOrientacaoDoRecurso("assistente/atendimento-empresa.md");

        assertThat(orientacao).contains("Liberdade de conversa")
                .contains("Inventar preço, horário, regra ou disponibilidade")
                .contains("nunca confirma nem promete uma reserva")
                .contains("outra empresa");
        String comercial = servicoIA.carregarOrientacaoDoRecurso("assistente/base-comercial.md");
        assertThat(comercial).contains("Como adaptar a Gamb ao negócio")
                .contains("Informar valor ou faixa de preço")
                .contains("Prometer prazo de implantação, vaga ou garantia")
                .contains("Citar nome de empresa cliente");
    }
}
