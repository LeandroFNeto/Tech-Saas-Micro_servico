package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Payload tipado recebido do WPPConnect via webhook. Aceita o formato plano (from/body) e o aninhado (data.key / data.message).")
public record WhatsappWebhookDTO(
        @Schema(description = "Nome da sessão WPPConnect da empresa", example = "sessao_recanto_01")
        String session,

        @Schema(description = "Tipo da mensagem", example = "chat")
        String type,

        @Schema(description = "Indica se a mensagem foi enviada pelo próprio número conectado (dono do bot)", example = "false")
        Boolean fromMe,

        @Schema(description = "JID do remetente no formato plano do WPPConnect", example = "5511999999999@c.us")
        String from,

        @Schema(description = "Texto da mensagem no formato plano do WPPConnect", example = "Olá, gostaria de reservar")
        String body,

        @Schema(description = "JID do remetente quando informado no topo do payload", example = "5511999999999@c.us")
        String remoteJid,

        @Schema(description = "Remetente alternativo, usado como fallback para JIDs @lid")
        String sender,

        @Schema(description = "Evento do WPPConnect quando não é mensagem de chat (qrcode, status-find, onack)", example = "qrcode")
        String event,

        @Schema(description = "Base64 do QR Code enviado no webhook de conexão")
        String qrcode,

        @Schema(description = "Identificador da mensagem (pode trazer fromMe em alguns provedores)")
        IdentificadorMensagem id,

        @Schema(description = "Envelope aninhado usado por alguns formatos do WPPConnect/Baileys")
        DadosWebhook data,

        @Schema(description = "Conteúdo tipado da mensagem, quando enviado em objeto")
        ConteudoMensagem content
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Identificador da mensagem no provedor")
    public record IdentificadorMensagem(
            @Schema(description = "Indica se a mensagem partiu do próprio número conectado", example = "false")
            Boolean fromMe,

            @Schema(description = "JID associado ao identificador", example = "5511999999999@c.us")
            String remoteJid
    ) {
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
    @Schema(description = "Conteúdo da mensagem em objeto")
    public record ConteudoMensagem(
            @Schema(description = "Texto da mensagem", example = "Olá, gostaria de reservar")
            String body
    ) {
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
        if (jid.contains("@lid")) {
            return temTexto(sender) ? sender : jid.split("@")[0] + "@s.whatsapp.net";
        }
        return jid;
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
