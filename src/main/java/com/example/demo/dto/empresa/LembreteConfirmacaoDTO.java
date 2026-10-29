package com.example.demo.dto.empresa;

import com.example.demo.model.ModoLembrete;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Configuração do lembrete de confirmação de reservas da empresa.")
public record LembreteConfirmacaoDTO(
        @Schema(description = "Modo de envio do lembrete", example = "DESLIGADO")
        ModoLembrete modo,

        @Min(value = 1, message = "Dias antes deve ser no mínimo 1")
        @Max(value = 3, message = "Dias antes deve ser no máximo 3")
        @Schema(description = "Quantidade de dias de antecedência para enviar o lembrete (1 a 3)", example = "1")
        Integer diasAntes,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de envio deve estar no formato HH:MM")
        @Schema(description = "Horário de envio no fuso de Brasília (HH:MM)", example = "10:00")
        String horarioEnvio,

        @Size(max = 1000, message = "O texto do lembrete não pode exceder 1000 caracteres")
        @Schema(description = "Texto do lembrete com variáveis {nome}, {data} e {horario}", maxLength = 1000)
        String texto
) {
}
