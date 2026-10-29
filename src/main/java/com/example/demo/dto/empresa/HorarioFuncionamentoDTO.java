package com.example.demo.dto.empresa;

import com.example.demo.model.PerfilFuncionamento;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Horário de funcionamento de um perfil: dia da semana ou FERIADO.")
public record HorarioFuncionamentoDTO(
        @Schema(description = "Perfil do horário", example = "SABADO")
        PerfilFuncionamento perfil,

        @Schema(description = "Se a empresa não abre neste perfil", example = "false")
        boolean fechado,

        @Schema(description = "Hora de abertura, HH:mm", example = "10:00")
        String abre,

        @Schema(description = "Hora de fechamento, HH:mm", example = "22:00")
        String fecha,

        @Schema(description = "Duração do atendimento neste perfil, em minutos; vazio herda a da empresa", example = "120")
        Integer duracaoMinutos
) {
}
