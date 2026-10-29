package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dia especial da empresa: feriado municipal, folga, evento privado ou horário estendido.")
public record DataEspecialCreateDTO(
        @Schema(description = "Data no formato AAAA-MM-DD", example = "2026-12-08")
        String data,

        @Schema(description = "Se a empresa fecha neste dia", example = "true")
        boolean fechado,

        @Schema(description = "Hora de abertura, HH:mm; obrigatória quando não fecha", example = "10:00")
        String abre,

        @Schema(description = "Hora de fechamento, HH:mm; obrigatória quando não fecha", example = "16:00")
        String fecha,

        @Schema(description = "Motivo mostrado ao cliente final", example = "Feriado municipal")
        String motivo
) {
}
