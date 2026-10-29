package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Funcionamento da reserva por hora de uma empresa.")
public record FuncionamentoDTO(
        @Schema(description = "Duração do atendimento em minutos", example = "60")
        int duracaoAtendimentoMinutos,

        @Schema(description = "Intervalo entre atendimentos em minutos", example = "0")
        int intervaloEntreAtendimentosMinutos,

        @Schema(description = "Sigla do estado da empresa", example = "SP")
        String uf,

        List<HorarioFuncionamentoDTO> horarios,

        List<DataEspecialDTO> diasEspeciais
) {
}
