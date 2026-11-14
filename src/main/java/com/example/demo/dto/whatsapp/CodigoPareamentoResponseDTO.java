package com.example.demo.dto.whatsapp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Código de 8 caracteres para digitar no celular, alternativa ao QR Code.")
public record CodigoPareamentoResponseDTO(
        @Schema(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
        String sessao,

        @Schema(description = "Código de pareamento no formato ABCD-EFGH, válido enquanto a sessão aguarda o pareamento",
                example = "ABCD-EFGH")
        String codigo
) {
}
