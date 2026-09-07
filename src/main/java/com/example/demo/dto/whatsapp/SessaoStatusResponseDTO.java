package com.example.demo.dto.whatsapp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status da sessão WhatsApp no WPPConnect, exposto ao painel sem vazar token interno.")
public record SessaoStatusResponseDTO(
        @Schema(description = "Identificador da sessão no WPPConnect", example = "sessao_recanto_01")
        String sessao,

        @Schema(description = "Estado normalizado para o front-end", example = "QRCODE",
                allowableValues = {"CONNECTED", "QRCODE", "DISCONNECTED"})
        String status,

        @Schema(description = "Imagem do QR Code em Data URL ou Base64, presente apenas quando status é QRCODE",
                nullable = true)
        String qrcodeBase64
) {
}
