package com.example.demo.dto.assistente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Mensagem do visitante enviada ao assistente público do login e da apresentação")
public record SuportePublicoRequestDTO(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "Pergunta do visitante", example = "Quem é a Gamb?", maxLength = 500)
        String mensagem,
        @Pattern(regexp = "LOGIN|APRESENTACAO")
        @Schema(description = "Tela de origem do chat; define as sugestões devolvidas. Sem valor, vale APRESENTACAO", allowableValues = {"LOGIN", "APRESENTACAO"}, example = "LOGIN")
        String origem
) {
}
