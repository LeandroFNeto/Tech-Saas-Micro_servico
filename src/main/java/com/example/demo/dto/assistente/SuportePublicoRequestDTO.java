package com.example.demo.dto.assistente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dúvida enviada ao bot de Suporte público")
public record SuportePublicoRequestDTO(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "Dúvida sobre o uso do app", example = "Como conecto o meu WhatsApp?", maxLength = 500)
        String mensagem
) {
}
