package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoMensagemDestinoTest {

    @Test
    @DisplayName("Chat @lid vai no body com isLid true para o WPPConnect não forçar @c.us")
    void marcaIsLidQuandoJidForLid() {
        Map<String, Object> body = new HashMap<>();
        ServicoMensagem.preencherDestino(body, "83717620011059@lid");

        assertThat(body.get("phone")).isEqualTo("83717620011059");
        assertThat(body.get("isLid")).isEqualTo(true);
        assertThat(body.get("isGroup")).isEqualTo(false);
    }

    @Test
    @DisplayName("Chat @c.us continua sem isLid")
    void naoMarcaIsLidQuandoForTelefone() {
        Map<String, Object> body = new HashMap<>();
        ServicoMensagem.preencherDestino(body, "5511988887777@c.us");

        assertThat(body.get("phone")).isEqualTo("5511988887777");
        assertThat(body.get("isLid")).isEqualTo(false);
    }
}
