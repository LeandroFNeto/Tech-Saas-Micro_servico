package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Configuração de funcionamento da reserva por hora. Campos nulos não são alterados.")
public record FuncionamentoUpdateDTO(
        @Schema(description = "Duração do atendimento em minutos, múltiplo de 30, de 30 a 720", example = "60")
        Integer duracaoAtendimentoMinutos,

        @Schema(description = "Intervalo entre atendimentos em minutos, de 0 a 120", example = "15")
        Integer intervaloEntreAtendimentosMinutos,

        @Schema(description = "Sigla do estado da empresa; vazio remove", example = "SP")
        String uf,

        @Schema(description = "Horários por perfil; a lista enviada substitui a anterior")
        List<HorarioFuncionamentoDTO> horarios
) {
}
