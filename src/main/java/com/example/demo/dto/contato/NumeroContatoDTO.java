package com.example.demo.dto.contato;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Número completo do contato descriptografado, retornado mediante auditoria.")
public record NumeroContatoDTO(
        @Schema(description = "Número completo do WhatsApp/telefone", example = "5511987654321")
        String numero
) {
}
