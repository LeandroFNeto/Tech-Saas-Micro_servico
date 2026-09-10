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
    @DisplayName("extrairApenasNumeros remove o sufixo do JID")
    void extraiNumerosDoJid() {
        assertThat(WhatsappUtil.extrairApenasNumeros("83717620011059@lid")).isEqualTo("83717620011059");
        assertThat(WhatsappUtil.extrairApenasNumeros("5511988887777@c.us")).isEqualTo("5511988887777");
    }
}
