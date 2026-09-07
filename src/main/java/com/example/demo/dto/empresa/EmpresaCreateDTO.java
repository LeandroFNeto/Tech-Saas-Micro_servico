package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

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

        @Schema(description = "Módulos liberados na criação da empresa", example = "[\"IA_GEMINI\", \"GOOGLE_CALENDAR\"]")
        List<String> modulosIniciais
) {
}
