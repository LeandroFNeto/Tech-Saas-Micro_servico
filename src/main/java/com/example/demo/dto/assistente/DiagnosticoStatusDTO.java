package com.example.demo.dto.assistente;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de integridade de um módulo verificado no diagnóstico do sistema")
public record DiagnosticoStatusDTO(
        @Schema(description = "Nome identificador do módulo ou subsistema verificado", example = "Auth")
        String modulo,

        @Schema(description = "Estado operacional apurado no teste de saúde", example = "OPERACIONAL")
        String status,

        @Schema(description = "Detalhes ou diagnóstico do teste de conectividade e funcionamento", example = "Conectividade e emissão de tokens validadas com sucesso.")
        String mensagem,

        @Schema(description = "Tempo de resposta medido em milissegundos", example = "45")
        Long tempoRespostaMs
) {
}
