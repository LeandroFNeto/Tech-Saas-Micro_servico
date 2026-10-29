package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dia especial cadastrado, com o aviso das reservas já existentes na data.")
public record DataEspecialDTO(
        Long id,

        @Schema(description = "Data no formato AAAA-MM-DD", example = "2026-12-08")
        String data,

        boolean fechado,

        @Schema(description = "Hora de abertura, HH:mm", example = "10:00")
        String abre,

        @Schema(description = "Hora de fechamento, HH:mm", example = "16:00")
        String fecha,

        @Schema(description = "Motivo mostrado ao cliente final")
        String motivo,

        @Schema(description = "Eventos que já existem na agenda nesta data quando ela é fechada; nada é cancelado")
        List<String> reservasNaData
) {
}
