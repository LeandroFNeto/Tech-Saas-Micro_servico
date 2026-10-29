package com.example.demo.dto.reserva;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para vincular um contato existente ou novo a uma reserva.")
public record VincularContatoDTO(
        @Schema(description = "ID de um contato já existente", example = "10")
        Long contatoId,

        @Schema(description = "Número de telefone brasileiro para criar e vincular novo contato", example = "11987654321")
        String telefone
) {
}
