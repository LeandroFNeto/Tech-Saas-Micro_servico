package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConversaCurtaTest {

    private final ConversaCurta conversa = new ConversaCurta();

    @ParameterizedTest
    @ValueSource(strings = {"oi", "Oii!", "Olá", "bom dia", "Boa tarde!", "boa noite", "e aí", "oi gente"})
    @DisplayName("Reconhece cumprimento")
    void reconheceCumprimento(String texto) {
        Optional<ConversaCurta.Resposta> r = conversa.responder("5511", texto);
        assertThat(r).isPresent();
        assertThat(r.get().tipo()).isEqualTo(ConversaCurta.Tipo.CUMPRIMENTO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"oi, tudo bem?", "bom dia tudo bem?", "tudo bom?", "como vai"})
    @DisplayName("Reconhece pergunta de bem-estar")
    void reconheceBemEstar(String texto) {
        assertThat(conversa.responder("5511", texto)).get()
                .extracting(ConversaCurta.Resposta::tipo).isEqualTo(ConversaCurta.Tipo.BEM_ESTAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"obrigado", "Muito obrigada!", "valeu", "vlw"})
    @DisplayName("Reconhece agradecimento")
    void reconheceAgradecimento(String texto) {
        assertThat(conversa.responder("5511", texto)).get()
                .extracting(ConversaCurta.Resposta::tipo).isEqualTo(ConversaCurta.Tipo.AGRADECIMENTO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"tchau", "até mais", "valeu, tchau"})
    @DisplayName("Reconhece despedida")
    void reconheceDespedida(String texto) {
        assertThat(conversa.responder("5511", texto)).get()
                .extracting(ConversaCurta.Resposta::tipo).isEqualTo(ConversaCurta.Tipo.DESPEDIDA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bom dia, quanto custa a diária?", "oi, tenho uma clínica de estética", "como está a reserva", "1", "menu", ""})
    @DisplayName("Mensagem com conteúdo ou comando não é conversa curta")
    void naoTrataMensagemComConteudo(String texto) {
        assertThat(conversa.responder("5511", texto)).isEmpty();
    }

    @Test
    @DisplayName("Bom dia responde bom dia")
    void respondeNoTurnoDoCliente() {
        assertThat(conversa.responder("5511", "bom dia").get().texto()).startsWith("Bom dia");
        assertThat(conversa.responder("5511", "boa noite").get().texto()).startsWith("Boa noite");
    }

    @Test
    @DisplayName("Não repete a mesma frase em cumprimentos seguidos do mesmo contato")
    void naoRepeteFraseSeguida() {
        for (int i = 0; i < 30; i++) {
            String a = conversa.responder("5511", "oi").get().texto();
            String b = conversa.responder("5511", "oi").get().texto();
            assertThat(b).isNotEqualTo(a);
        }
    }
}
