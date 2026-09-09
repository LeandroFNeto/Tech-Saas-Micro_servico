package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Payload tipado recebido do WPPConnect via webhook. Aceita o formato plano (from/body), o aninhado (data.key / data.message) e id/sender/content ora string, ora objeto.")
public record WhatsappWebhookDTO(
        @Schema(description = "Nome da sessão WPPConnect da empresa", example = "sessao_recanto_01")
        String session,

        @JsonProperty("type")
        @Schema(description = "Tipo da mensagem", example = "chat")
        String type,

        @JsonProperty("fromMe")
        @Schema(description = "Indica se a mensagem foi enviada pelo próprio número conectado (dono do bot)", example = "false")
        Boolean fromMe,

        @JsonProperty("isGroupMsg")
        @JsonAlias("isGroup")
        @Schema(description = "Indica se a mensagem veio de um grupo. False/ausente em chats privados, inclusive @lid", example = "false")
        Boolean isGroupMsg,

        @JsonProperty("from")
        @Schema(description = "JID do remetente no formato plano do WPPConnect", example = "5511999999999@c.us")
        String from,

        @JsonProperty("body")
        @Schema(description = "Texto da mensagem no formato plano do WPPConnect", example = "Olá, gostaria de reservar")
        String body,

        @Schema(description = "JID do remetente quando informado no topo do payload", example = "5511999999999@c.us")
        String remoteJid,

        @Schema(description = "Remetente: JID em string ou objeto Contact do WPPConnect")
        RemetenteWebhook sender,

        @Schema(description = "Evento do WPPConnect quando não é mensagem de chat (qrcode, status-find, onack)", example = "qrcode")
        String event,

        @Schema(description = "Base64 do QR Code enviado no webhook de conexão")
        String qrcode,

        @Schema(description = "Identificador da mensagem: string serializada (onmessage) ou objeto Baileys (onack)")
        IdentificadorMensagem id,

        @Schema(description = "Envelope aninhado usado por alguns formatos do WPPConnect/Baileys")
        DadosWebhook data,

        @Schema(description = "Conteúdo da mensagem: string do WPPConnect ou objeto com body")
        ConteudoMensagem content
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = IdentificadorMensagem.Deserializer.class)
    @Schema(description = "Identificador da mensagem no provedor. Aceita string serializada ou objeto com fromMe/remote/_serialized.")
    public record IdentificadorMensagem(
            @Schema(description = "Indica se a mensagem partiu do próprio número conectado", example = "false")
            Boolean fromMe,

            @Schema(description = "JID associado ao identificador", example = "5511999999999@c.us")
            String remoteJid,

            @JsonProperty("_serialized")
            @Schema(description = "ID serializado no formato fromMe_jid_hash", example = "false_5511999999999@c.us_3AE7951B92607B7B73F8")
            String serialized
    ) {
        static IdentificadorMensagem deJson(JsonNode node) {
            if (node == null || node.isNull() || node.isMissingNode()) {
                return null;
            }
            if (node.isTextual() || node.isNumber()) {
                return deTexto(node.asText());
            }
            if (!node.isObject()) {
                return null;
            }
            Boolean fromMe = booleano(node, "fromMe");
            String remoteJid = textoFlexivel(node.get("remoteJid"));
            if (remoteJid == null) {
                remoteJid = textoFlexivel(node.get("remote"));
            }
            String serialized = textoFlexivel(node.get("_serialized"));
            if (remoteJid == null && serialized != null) {
                IdentificadorMensagem extraido = deTexto(serialized);
                if (extraido != null) {
                    if (fromMe == null) {
                        fromMe = extraido.fromMe();
                    }
                    remoteJid = extraido.remoteJid();
                }
            }
            return new IdentificadorMensagem(fromMe, remoteJid, serialized);
        }

        static IdentificadorMensagem deTexto(String valor) {
            if (valor == null || valor.isBlank()) {
                return null;
            }
            String texto = valor.trim();
            boolean temPrefixo = texto.startsWith("true_") || texto.startsWith("false_");
            if (!temPrefixo) {
                return new IdentificadorMensagem(null, texto.contains("@") ? texto : null, texto);
            }
            Boolean fromMe = texto.startsWith("true_");
            String resto = texto.substring(texto.indexOf('_') + 1);
            int arroba = resto.indexOf('@');
            if (arroba < 0) {
                return new IdentificadorMensagem(fromMe, null, texto);
            }
            int fimJid = resto.indexOf('_', arroba);
            String remoteJid = fimJid < 0 ? resto : resto.substring(0, fimJid);
            return new IdentificadorMensagem(fromMe, remoteJid, texto);
        }

        public static class Deserializer extends JsonDeserializer<IdentificadorMensagem> {
            @Override
            public IdentificadorMensagem deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return deJson(p.readValueAsTree());
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = RemetenteWebhook.Deserializer.class)
    @Schema(description = "Contato remetente do WPPConnect. Aceita JID string ou objeto Contact.")
    public record RemetenteWebhook(
            @Schema(description = "JID do remetente", example = "5511999999999@c.us")
            String id
    ) {
        public static class Deserializer extends JsonDeserializer<RemetenteWebhook> {
            @Override
            public RemetenteWebhook deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonNode node = p.readValueAsTree();
                if (node == null || node.isNull() || node.isMissingNode()) {
                    return null;
                }
                if (node.isTextual()) {
                    return new RemetenteWebhook(node.asText());
                }
                String id = textoFlexivel(node.get("id"));
                if (id == null) {
                    id = textoFlexivel(node);
                }
                return new RemetenteWebhook(id);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Dados aninhados da mensagem")
    public record DadosWebhook(
            @Schema(description = "Chave da mensagem com o JID do remetente")
            ChaveMensagem key,

            @Schema(description = "Corpo aninhado da mensagem")
            MensagemAninhada message
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Chave da mensagem no formato Baileys")
    public record ChaveMensagem(
            @Schema(description = "JID do remetente", example = "5511999999999@c.us")
            String remoteJid,

            @Schema(description = "Indica se a mensagem foi enviada pelo dono do bot", example = "false")
            Boolean fromMe
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Mensagem no formato aninhado")
    public record MensagemAninhada(
            @Schema(description = "Texto da conversa", example = "Olá, gostaria de reservar")
            String conversation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = ConteudoMensagem.Deserializer.class)
    @Schema(description = "Conteúdo da mensagem. Aceita string do WPPConnect ou objeto com body.")
    public record ConteudoMensagem(
            @Schema(description = "Texto da mensagem", example = "Olá, gostaria de reservar")
            String body
    ) {
        public static class Deserializer extends JsonDeserializer<ConteudoMensagem> {
            @Override
            public ConteudoMensagem deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonNode node = p.readValueAsTree();
                if (node == null || node.isNull() || node.isMissingNode()) {
                    return null;
                }
                if (node.isTextual()) {
                    return new ConteudoMensagem(node.asText());
                }
                if (!node.isObject()) {
                    return null;
                }
                String body = textoFlexivel(node.get("body"));
                if (body == null) {
                    body = textoFlexivel(node.get("conversation"));
                }
                return new ConteudoMensagem(body);
            }
        }
    }

    public String resumoRaioX() {
        String idLog = id == null ? null : (temTexto(id.serialized()) ? id.serialized() : id.remoteJid());
        String contentLog = content == null ? null : content.body();
        return String.format(
                "🔬 [RAIO-X] ID=%s | type=%s | isGroupMsg=%s | fromMe=%s | from=%s | body=%s | content=%s",
                idLog, type, isGroupMsg, fromMe, from, body, contentLog);
    }

    public boolean deveDescartarComoRuido() {
        if (ehGrupo() || ehStatus()) {
            return true;
        }
        if (!ehMensagemDeTexto()) {
            return true;
        }
        return !temTexto(resolverRemoteJid());
    }

    public boolean ehGrupo() {
        if (Boolean.TRUE.equals(isGroupMsg)) {
            return true;
        }
        if (temTexto(from) && from.contains("@g.us")) {
            return true;
        }
        String jid = resolverRemoteJid();
        return jid != null && jid.contains("@g.us");
    }

    public boolean ehStatus() {
        if ("status".equalsIgnoreCase(type)) {
            return true;
        }
        return jidDeStatus(from) || jidDeStatus(resolverRemoteJid());
    }

    public boolean ehMensagemDeTexto() {
        if (!temTexto(type)) {
            return temTexto(resolverTexto());
        }
        return "chat".equalsIgnoreCase(type.trim());
    }

    public boolean ehEventoQrcode() {
        if (qrcode != null && !qrcode.isBlank()) {
            return true;
        }
        return "qrcode".equalsIgnoreCase(event) || "qrcode".equalsIgnoreCase(type);
    }

    public boolean ehEventoConexao() {
        String evento = event != null ? event : type;
        if (evento == null || evento.isBlank()) {
            return false;
        }
        String valor = evento.trim().toLowerCase();
        return valor.contains("status-find")
                || valor.contains("statusfind")
                || valor.contains("autoclose")
                || valor.contains("browserclose")
                || valor.equals("onack")
                || valor.equals("onpresencechanged")
                || valor.equals("onparticipantschanged")
                || valor.equals("incomingcall")
                || valor.equals("closesession")
                || valor.equals("onreactionmessage")
                || valor.equals("onrevokedmessage")
                || valor.equals("onpollresponse")
                || valor.equals("onupdatelabel")
                || valor.equals("onselfmessage");
    }

    public boolean enviadaPorMim() {
        if (fromMe != null) {
            return fromMe;
        }
        if (id != null && id.fromMe() != null) {
            return id.fromMe();
        }
        if (data != null && data.key() != null && data.key().fromMe() != null) {
            return data.key().fromMe();
        }
        return false;
    }

    public String resolverRemoteJid() {
        if (temTexto(remoteJid)) {
            return resolverLid(remoteJid);
        }
        if (temTexto(from)) {
            return resolverLid(from);
        }
        if (data != null && data.key() != null && temTexto(data.key().remoteJid())) {
            return resolverLid(data.key().remoteJid());
        }
        if (id != null && temTexto(id.remoteJid())) {
            return resolverLid(id.remoteJid());
        }
        if (sender != null && temTexto(sender.id())) {
            return resolverLid(sender.id());
        }
        return null;
    }

    public String resolverTexto() {
        if (temTexto(body)) {
            return body;
        }
        if (content != null && temTexto(content.body())) {
            return content.body();
        }
        if (data != null && data.message() != null) {
            return data.message().conversation();
        }
        return null;
    }

    private String resolverLid(String jid) {
        if (!jid.contains("@lid")) {
            return jid;
        }
        String jidSender = sender != null ? sender.id() : null;
        if (temTexto(jidSender) && (jidSender.contains("@c.us") || jidSender.contains("@s.whatsapp.net"))) {
            return jidSender;
        }
        return jid;
    }

    private static boolean jidDeStatus(String jid) {
        if (!temTexto(jid)) {
            return false;
        }
        String valor = jid.toLowerCase();
        return valor.contains("status@") || valor.contains("@broadcast");
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Boolean booleano(JsonNode node, String campo) {
        if (node == null || !node.has(campo) || node.get(campo).isNull()) {
            return null;
        }
        JsonNode valor = node.get(campo);
        if (valor.isBoolean()) {
            return valor.booleanValue();
        }
        if (valor.isTextual()) {
            return Boolean.valueOf(valor.asText());
        }
        return null;
    }

    private static String textoFlexivel(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            String texto = node.asText();
            return texto.isBlank() ? null : texto;
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject()) {
            if (node.hasNonNull("_serialized") && node.get("_serialized").isTextual()) {
                String serialized = node.get("_serialized").asText();
                return serialized.isBlank() ? null : serialized;
            }
            if (node.hasNonNull("user") && node.hasNonNull("server")) {
                return node.get("user").asText() + "@" + node.get("server").asText();
            }
        }
        return null;
    }
}
