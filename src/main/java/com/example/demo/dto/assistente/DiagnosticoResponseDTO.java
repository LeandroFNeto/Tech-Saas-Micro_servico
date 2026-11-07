package com.example.demo.dto.assistente;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Relatório consolidado do diagnóstico de integridade de todos os módulos")
public record DiagnosticoResponseDTO(
        @Schema(description = "Status global consolidado da plataforma", example = "OPERACIONAL")
        String statusGeral,

        @Schema(description = "Data e hora da execução da rotina de diagnóstico", example = "2026-11-03T14:30:00Z")
        Instant dataHora,

        @Schema(description = "Lista com os resultados individuais de cada módulo testado")
        List<DiagnosticoStatusDTO> modulos
) {
}
