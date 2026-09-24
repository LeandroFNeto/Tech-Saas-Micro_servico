package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nova senha definida pelo administrador, sem conferir a senha atual do cliente.")
public record ResetSenhaAdminDTO(
        @NotBlank
        @Size(min = 6)
        @Schema(description = "Senha em texto puro; o servidor persiste apenas o hash BCrypt", example = "NovaSenha456")
        String novaSenha
) {
}
