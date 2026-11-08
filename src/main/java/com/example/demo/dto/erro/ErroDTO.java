package com.example.demo.dto.erro;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corpo de erro da API, sem mensagem interna nem rastro de pilha.")
public record ErroDTO(
        @Schema(description = "Texto pensado para o usuário", example = "Erro interno")
        String message
) {
}
