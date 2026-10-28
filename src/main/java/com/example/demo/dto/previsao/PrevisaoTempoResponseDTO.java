package com.example.demo.dto.previsao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Previsão dos próximos dias da cidade da empresa, com os dias reservados marcados")
public record PrevisaoTempoResponseDTO(
        @Schema(description = "false quando o interruptor está desligado, sem cidade, sem chave da API ou o provedor falhou sem cache", example = "true")
        boolean disponivel,

        @Schema(description = "false quando a agenda está configurada e o Google Agenda falhou", example = "true")
        boolean reservasDisponiveis,

        @Schema(description = "Horário real da entrada de cache usada, em America/Sao_Paulo", example = "2026-10-27T14:30:00")
        LocalDateTime atualizadoEm,

        @Schema(description = "Nome resolvido da cidade", example = "Atibaia, São Paulo, BR")
        String cidade,

        @Schema(description = "Até 8 dias a partir de hoje; vazia quando disponivel é false")
        List<PrevisaoDiaDTO> dias
) {
}
