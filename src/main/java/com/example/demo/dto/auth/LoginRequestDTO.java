package com.example.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais enviadas no POST /auth/login. Não expõe entidade JPA.")
public record LoginRequestDTO(
        @NotBlank
        @Email
        @Schema(description = "E-mail cadastrado do usuário", example = "usuario@gamb.com")
        String email,

        @NotBlank
        @Schema(description = "Senha em texto puro; o servidor compara com o hash BCrypt", example = "SenhaMaster123")
        String senha
) {
}
