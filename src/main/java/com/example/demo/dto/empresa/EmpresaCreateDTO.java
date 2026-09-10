package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Contrato de entrada para cadastrar uma nova empresa no SaaS (POST)")
public record EmpresaCreateDTO(
        @NotBlank
        @Schema(description = "Nome fantasia do estabelecimento", example = "Recanto Vista Alegre")
        String nome,

        @NotBlank
        @Schema(description = "Identificador único da sessão no WPPConnect", example = "sessao_recanto_01")
        String sessaoWhatsapp,

        @Schema(description = "Ramo de atuação usado para escolher a estratégia de atendimento", example = "locacao")
        String ramoDeAtuacao,

        @Schema(description = "Preço base de referência da locação", example = "500.00")
        Double precoBase,

        @Schema(description = "ID da agenda Google desta empresa (não usar valor global no .env)",
                example = "abc123@group.calendar.google.com")
        String googleCalendarId,

        @Schema(description = "Quando true, o bot já nasce com a reserva automática habilitada no WhatsApp",
                example = "false")
        Boolean permiteReservaAutomatica,

        @Schema(description = "URL pública da foto principal (Hero Image) hospedada no Cloudinary",
                example = "https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg")
        String linkFotoPrincipal,

        @Schema(description = "Lista de URLs públicas das fotos complementares hospedadas no Cloudinary",
                example = "[\"https://res.cloudinary.com/demo/image/upload/v1/foto1.jpg\"]")
        @Size(max = 5)
        List<String> urlsGaleria,

        @Schema(description = "E-mail de acesso do cliente. O servidor ignora o valor enviado e força sessaoWhatsapp (minúsculas, sem espaços) + @gamb.com.br.",
                example = "sessao_recanto_01@gamb.com.br")
        String email,

        @NotBlank
        @Size(min = 6)
        @Schema(description = "Senha inicial do cliente; o servidor persiste apenas o hash BCrypt",
                example = "SenhaCliente123")
        String senha,

        @Schema(description = "Módulos liberados na criação da empresa", example = "[\"IA_GEMINI\", \"GOOGLE_CALENDAR\"]")
        List<String> modulosIniciais
) {
}
