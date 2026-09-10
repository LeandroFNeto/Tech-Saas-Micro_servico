package com.example.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Troca de senha do usuário autenticado (PUT /auth/alterar-senha).")
public record AlterarSenhaDTO(
        @NotBlank
        @Schema(description = "Senha atual em texto puro", example = "SenhaMaster123")
        String senhaAtual,

        @NotBlank
        @Size(min = 6)
        @Schema(description = "Nova senha; o servidor persiste o hash BCrypt", example = "NovaSenha456")
        String novaSenha
) {
}
