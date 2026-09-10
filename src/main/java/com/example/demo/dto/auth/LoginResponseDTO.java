package com.example.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do login com JWT. Não inclui a senha nem a entidade JPA.")
public record LoginResponseDTO(
        @Schema(description = "JWT assinado para Authorization Bearer")
        String token,

        @Schema(description = "E-mail autenticado", example = "usuario@gamb.com")
        String email,

        @Schema(description = "Papel do usuário no SaaS", example = "ADMIN")
        String role,

        @Schema(description = "Nome exibido no painel")
        String nome,

        @Schema(description = "Sessão WPPConnect da empresa quando o papel é CLIENTE")
        String sessaoWhatsapp
) {
}
