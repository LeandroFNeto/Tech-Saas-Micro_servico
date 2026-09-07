package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Contrato de atualização administrativa (PUT). Campos de infraestrutura e módulos; não deve ser usado pelo painel do cliente.")
public record EmpresaUpdateAdminDTO(
        @Schema(description = "Identificador da sessão que conecta com o WPPConnect", example = "sessao_recanto_01")
        String sessaoWhatsapp,

        @Schema(description = "Define se o local é alugado por hora (true) ou por diária (false)", example = "false")
        Boolean locacaoPorHora,

        @Schema(description = "ID da agenda Google desta empresa (não usar valor global no .env)",
                example = "abc123@group.calendar.google.com")
        String googleCalendarId,

        @Schema(description = "Lista de módulos liberados após pagamento", example = "[\"IA_GEMINI\", \"GOOGLE_CALENDAR\"]")
        List<String> modulosAtivos
) {
}
