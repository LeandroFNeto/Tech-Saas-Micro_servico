package com.example.demo.dto.whatsapp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status da sessão WhatsApp no gateway do WhatsApp, exposto ao painel sem vazar token interno.")
public record SessaoStatusResponseDTO(
        @Schema(description = "Identificador da sessão no gateway do WhatsApp", example = "sessao_recanto_01")
        String sessao,

        @Schema(description = "Estado normalizado para o front-end", example = "QRCODE",
                allowableValues = {"CONNECTED", "QRCODE", "INITIALIZING", "DISCONNECTED"})
        String status,

        @Schema(description = "Imagem do QR Code em Data URL ou Base64, presente apenas quando status é QRCODE",
                nullable = true)
        String qrcodeBase64,

        @Schema(description = "Verdadeiro só com status DISCONNECTED quando o gateway registrou o encerramento do pareamento; ausente no gateway vira falso",
                defaultValue = "false")
        boolean sessaoEncerrada
) {
    public SessaoStatusResponseDTO(String sessao, String status, String qrcodeBase64) {
        this(sessao, status, qrcodeBase64, false);
    }
}
