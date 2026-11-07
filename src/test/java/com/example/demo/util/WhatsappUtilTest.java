package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappUtilTest {

    @Test
    @DisplayName("JID @lid é reconhecido para o WPPConnect enviar com isLid")
    void reconheceJidLid() {
        assertThat(WhatsappUtil.isLid("83717620011059@lid")).isTrue();
        assertThat(WhatsappUtil.isLid("5511988887777@c.us")).isFalse();
        assertThat(WhatsappUtil.isLid(null)).isFalse();
    }

    @Test
    @DisplayName("extrairApenasNumeros remove o sufixo do JID e trata '@' isolado sem exceção")
    void extraiNumerosDoJid() {
        assertThat(WhatsappUtil.extrairApenasNumeros("83717620011059@lid")).isEqualTo("83717620011059");
        assertThat(WhatsappUtil.extrairApenasNumeros("5511988887777@c.us")).isEqualTo("5511988887777");
        assertThat(WhatsappUtil.extrairApenasNumeros("@")).isEmpty();
        assertThat(WhatsappUtil.extrairApenasNumeros(null)).isEmpty();
    }

    @Test
    @DisplayName("normalizarParaChaveEstado preserva todos os dígitos de @lid de 13 dígitos e remove 9º dígito só em telefones 55")
    void normalizaChaveEstadoSemCorromperLid() {
        assertThat(WhatsappUtil.normalizarParaChaveEstado("5511988887777@lid")).isEqualTo("5511988887777");
        assertThat(WhatsappUtil.normalizarParaChaveEstado("5511988887777@c.us")).isEqualTo("551188887777");
        assertThat(WhatsappUtil.normalizarParaChaveEstado("@")).isEmpty();
    }
}
