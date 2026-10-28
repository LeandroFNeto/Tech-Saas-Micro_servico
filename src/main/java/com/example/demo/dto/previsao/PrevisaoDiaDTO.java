package com.example.demo.dto.previsao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Previsão de um dia, cruzada com a ocupação da agenda da empresa")
public record PrevisaoDiaDTO(
        @Schema(description = "Data do dia", example = "2026-10-30")
        LocalDate data,

        @Schema(description = "Nome do dia da semana", example = "sexta-feira")
        String diaSemana,

        @Schema(description = "true quando a data está ocupada no Google Agenda", example = "false")
        boolean reservado,

        @Schema(description = "Descrição curta do tempo", example = "chuva moderada")
        String resumo,

        @Schema(description = "Código de condição do provedor; 200 a 299 é temporal", example = "501")
        int codigoTempo,

        @Schema(description = "Temperatura máxima em Celsius", example = "31.4")
        double tempMax,

        @Schema(description = "Temperatura mínima em Celsius", example = "19.8")
        double tempMin,

        @Schema(description = "Chance de chuva em porcentagem", example = "72")
        int chanceChuva,

        @Schema(description = "Chuva prevista em milímetros", example = "4.5")
        double chuvaMm,

        @Schema(description = "Risco de desmarcação", allowableValues = {"BAIXO", "MEDIO", "ALTO"}, example = "ALTO")
        String risco,

        @Schema(description = "true do 6º ao 8º dia", example = "false")
        boolean estimativa
) {
}
