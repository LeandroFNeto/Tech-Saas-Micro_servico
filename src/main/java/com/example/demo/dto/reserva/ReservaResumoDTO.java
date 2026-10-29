package com.example.demo.dto.reserva;

import com.example.demo.model.OrigemReserva;
import com.example.demo.model.SituacaoReserva;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Resumo da reserva para exibição na aba Reservas do cliente.")
public record ReservaResumoDTO(
        @Schema(description = "Identificador único da reserva", example = "1")
        Long id,

        @Schema(description = "Data da reserva", example = "2026-11-15")
        LocalDate data,

        @Schema(description = "Horário da reserva ou 'Diária Completa'", example = "Diária Completa")
        String horario,

        @Schema(description = "Título da reserva ou evento", example = "Reserva - João Silva")
        String titulo,

        @Schema(description = "Nome do cliente associado à reserva", example = "João Silva")
        String nomeCliente,

        @Schema(description = "Situação da reserva e do lembrete", example = "AGUARDANDO_LEMBRETE")
        SituacaoReserva situacao,

        @Schema(description = "Indica se a reserva está em risco alto de perda", example = "false")
        boolean riscoPerda,

        @Schema(description = "Indica se a reserva requer atenção imediata", example = "false")
        boolean precisaAtencao,

        @Schema(description = "Origem da reserva (AGENDA ou BOT)", example = "AGENDA")
        OrigemReserva origem,

        @Schema(description = "Identificador do contato vinculado", example = "10")
        Long contatoId,

        @Schema(description = "Telefone mascarado para exibição segura", example = "(11) *****-1234")
        String telefoneMascarado,

        @Schema(description = "Se o lembrete pode ser enviado manualmente agora", example = "true")
        boolean podeAvisar
) {
}
