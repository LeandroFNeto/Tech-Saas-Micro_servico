package com.example.demo.dto.contato;

import com.example.demo.model.Contato;
import com.example.demo.model.OrigemContato;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumo do contato de uma empresa com telefone mascarado.")
public record ContatoResumoDTO(
        @Schema(description = "Identificador do contato", example = "1")
        Long id,

        @Schema(description = "Nome do contato quando conhecido", example = "João Silva")
        String nome,

        @Schema(description = "Telefone mascarado para proteção de dados", example = "(11) *****-1234")
        String telefoneMascarado,

        @Schema(description = "Data e hora do primeiro contato", example = "2026-10-01T10:00:00")
        LocalDateTime primeiraConversaEm,

        @Schema(description = "Data e hora da última conversa", example = "2026-10-28T15:30:00")
        LocalDateTime ultimaConversaEm,

        @Schema(description = "Indica se o contato solicitou parar o recebimento de mensagens", example = "false")
        boolean naoContatar,

        @Schema(description = "Origem de registro do contato", example = "CONVERSA")
        OrigemContato origem
) {
    public static ContatoResumoDTO fromEntity(Contato contato) {
        if (contato == null) {
            return null;
        }
        String mascara = formatarMascara(contato.getFinalTelefone());
        String nomeExibicao = contato.getNome();
        if (nomeExibicao == null || nomeExibicao.isBlank()) {
            nomeExibicao = "Contato " + mascara;
        }
        return new ContatoResumoDTO(
                contato.getId(),
                nomeExibicao,
                mascara,
                contato.getPrimeiraConversaEm(),
                contato.getUltimaConversaEm(),
                contato.isNaoContatar(),
                contato.getOrigem()
        );
    }

    private static String formatarMascara(String finalTelefone) {
        if (finalTelefone == null || finalTelefone.isBlank()) {
            return "(**) *****-****";
        }
        return "(**) *****-" + finalTelefone;
    }
}
