package com.example.demo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappWebhookDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Deserializa id serializado de onmessage (string) e sender/content mistos")
    void deveAceitarIdStringSenderObjetoEContentString() throws Exception {
        String json = """
            {
              "event": "onmessage",
              "session": "RecantoBot",
              "id": "false_120363421596195995@g.us_3AE7951B92607B7B73F8_185336529469528@lid",
              "from": "120363421596195995@g.us",
              "type": "chat",
              "fromMe": false,
              "body": "oi",
              "content": "oi",
              "sender": {
                "id": {
                  "server": "lid",
                  "user": "185336529469528",
                  "_serialized": "185336529469528@lid"
                },
                "pushname": "Maria"
              }
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.id().serialized()).isEqualTo(
                "false_120363421596195995@g.us_3AE7951B92607B7B73F8_185336529469528@lid");
        assertThat(dto.id().fromMe()).isFalse();
        assertThat(dto.id().remoteJid()).isEqualTo("120363421596195995@g.us");
        assertThat(dto.sender().id()).isEqualTo("185336529469528@lid");
        assertThat(dto.content().body()).isEqualTo("oi");
        assertThat(dto.resolverTexto()).isEqualTo("oi");
        assertThat(dto.resolverRemoteJid()).isEqualTo("120363421596195995@g.us");
        assertThat(dto.ehGrupo()).isTrue();
        assertThat(dto.deveDescartarComoRuido()).isTrue();
    }

    @Test
    @DisplayName("Deserializa id como JID @lid no evento onpresencechanged")
    void deveAceitarIdComoJidLid() throws Exception {
        String json = """
            {
              "event": "onpresencechanged",
              "session": "RecantoBot",
              "id": "185336529469528@lid"
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.ehEventoConexao()).isTrue();
        assertThat(dto.id().remoteJid()).isEqualTo("185336529469528@lid");
        assertThat(dto.id().serialized()).isEqualTo("185336529469528@lid");
    }

    @Test
    @DisplayName("Deserializa id objeto do onack com remote string e _serialized")
    void deveAceitarIdObjetoDoOnack() throws Exception {
        String json = """
            {
              "event": "onack",
              "session": "RecantoBot",
              "id": {
                "fromMe": true,
                "remote": "5511999999999@c.us",
                "id": "3EB0XXXX",
                "_serialized": "true_5511999999999@c.us_3EB0XXXX"
              },
              "ack": 3
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.ehEventoConexao()).isTrue();
        assertThat(dto.id().fromMe()).isTrue();
        assertThat(dto.id().remoteJid()).isEqualTo("5511999999999@c.us");
        assertThat(dto.id().serialized()).isEqualTo("true_5511999999999@c.us_3EB0XXXX");
        assertThat(dto.enviadaPorMim()).isTrue();
    }

    @Test
    @DisplayName("Deserializa remote do onack quando chega como objeto Wid")
    void deveAceitarRemoteComoObjetoWid() throws Exception {
        String json = """
            {
              "event": "onack",
              "session": "RecantoBot",
              "id": {
                "fromMe": false,
                "remote": {
                  "server": "c.us",
                  "user": "5511999999999",
                  "_serialized": "5511999999999@c.us"
                },
                "id": "ABC",
                "_serialized": "false_5511999999999@c.us_ABC"
              }
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.id().fromMe()).isFalse();
        assertThat(dto.id().remoteJid()).isEqualTo("5511999999999@c.us");
    }

    @Test
    @DisplayName("Deserializa sender como string e content como objeto")
    void deveAceitarSenderStringEContentObjeto() throws Exception {
        String json = """
            {
              "session": "RecantoBot",
              "from": "5511999999999@c.us",
              "type": "chat",
              "fromMe": false,
              "sender": "5511999999999@c.us",
              "content": { "body": "Quero reservar" }
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.sender().id()).isEqualTo("5511999999999@c.us");
        assertThat(dto.content().body()).isEqualTo("Quero reservar");
        assertThat(dto.resolverTexto()).isEqualTo("Quero reservar");
    }

    @Test
    @DisplayName("Chat privado @lid com isGroupMsg false não é tratado como grupo")
    void naoDeveDescartarChatPrivadoComLid() throws Exception {
        String json = """
            {
              "event": "onmessage",
              "session": "RecantoBot",
              "id": "false_276974622781686@lid_3AE7951B92607B7B73F8",
              "from": "276974622781686@lid",
              "type": "chat",
              "fromMe": false,
              "isGroupMsg": false,
              "body": "Oi, quero reservar",
              "content": "Oi, quero reservar"
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.resumoRaioX()).contains("type=chat").contains("isGroupMsg=false").contains("fromMe=false");
        assertThat(dto.ehGrupo()).isFalse();
        assertThat(dto.ehStatus()).isFalse();
        assertThat(dto.ehMensagemDeTexto()).isTrue();
        assertThat(dto.deveDescartarComoRuido()).isFalse();
        assertThat(dto.resolverRemoteJid()).isEqualTo("276974622781686@lid");
    }

    @Test
    @DisplayName("type nulo ainda aceita texto privado @c.us")
    void deveAceitarTextoQuandoTypeVierNulo() throws Exception {
        String json = """
            {
              "session": "RecantoBot",
              "from": "5511999999999@c.us",
              "fromMe": false,
              "body": "Olá"
            }
            """;

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);

        assertThat(dto.ehMensagemDeTexto()).isTrue();
        assertThat(dto.deveDescartarComoRuido()).isFalse();
    }
}
