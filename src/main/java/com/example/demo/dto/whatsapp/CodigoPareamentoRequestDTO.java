package com.example.demo.dto.whatsapp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Telefone do número que será conectado ao WhatsApp da empresa. Usado só para gerar o código e nunca gravado.")
public record CodigoPareamentoRequestDTO(
        @NotBlank
        @Pattern(regexp = "^55\\d{10,11}$", message = "deve ter 12 ou 13 dígitos com DDI 55")
        @Schema(description = "Somente dígitos, com DDI 55, DDD e número (12 ou 13 dígitos)", example = "5511999990000")
        String telefone
) {
}
