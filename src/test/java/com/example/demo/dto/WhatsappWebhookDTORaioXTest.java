package com.example.demo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappWebhookDTORaioXTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("resumoRaioX não traz body nem content e mascara o jid")
    void resumoNaoTrazTextoNemNumeroCompleto() throws Exception {
        String json = """
            {
              "event": "onmessage",
              "session": "RecantoBot",
              "from": "5511999997777@c.us",
              "type": "chat",
              "fromMe": false,
              "isGroupMsg": false,
              "body": "Quero reservar o salao para o aniversario da Joana",
              "content": "Quero reservar o salao para o aniversario da Joana"
            }
            """;

        String resumo = mapper.readValue(json, WhatsappWebhookDTO.class).resumoRaioX();

        assertThat(resumo)
                .contains("type=chat")
                .contains("isGroupMsg=false")
                .contains("fromMe=false")
                .contains("numero=***7777")
                .contains("texto=50 caracteres")
                .doesNotContain("Quero reservar")
                .doesNotContain("aniversario")
                .doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("resumoRaioX de jid @lid usa a máscara de lid")
    void resumoMascaraLid() throws Exception {
        String json = """
            {
              "session": "RecantoBot",
              "from": "276974622781686@lid",
              "type": "chat",
              "fromMe": false,
              "body": "oi"
            }
            """;

        String resumo = mapper.readValue(json, WhatsappWebhookDTO.class).resumoRaioX();

        assertThat(resumo).contains("numero=lid:***1686").doesNotContain("276974622781686");
    }

    @Test
    @DisplayName("resumoRaioX com content em objeto não traz o texto")
    void resumoComContentObjetoNaoTrazTexto() throws Exception {
        String json = """
            {
              "session": "RecantoBot",
              "from": "5511999997777@c.us",
              "type": "chat",
              "fromMe": false,
              "content": { "body": "texto sigiloso do cliente" }
            }
            """;

        String resumo = mapper.readValue(json, WhatsappWebhookDTO.class).resumoRaioX();

        assertThat(resumo).doesNotContain("sigiloso").contains("texto=25 caracteres");
    }

    @Test
    @DisplayName("resumoRaioX com id serializado não expõe o número completo")
    void resumoComIdSerializadoNaoExpoeNumero() throws Exception {
        String json = """
            {
              "session": "RecantoBot",
              "id": "false_5511999997777@c.us_3AE7951B92607B7B73F8",
              "from": "5511999997777@c.us",
              "type": "chat",
              "fromMe": false,
              "body": "oi"
            }
            """;

        String resumo = mapper.readValue(json, WhatsappWebhookDTO.class).resumoRaioX();

        assertThat(resumo).doesNotContain("5511999997777");
    }
}
