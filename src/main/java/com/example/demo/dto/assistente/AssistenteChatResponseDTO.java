package com.example.demo.dto.assistente;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Resposta gerada pela IA contextualizada na base de tutoria e avisos")
public record AssistenteChatResponseDTO(
        @Schema(description = "Resposta formulada pela IA com instruções, diagnóstico ou simulação", example = "Para cadastrar um novo aviso...")
        String resposta,

        @Schema(description = "Modo em que a resposta foi processada", example = "DIAGNOSTICO")
        String modo,

        @Schema(description = "Momento da geração da resposta", example = "2026-11-03T14:30:05Z")
        Instant dataHora,

        @Schema(description = "Sugestões de próximas ações ou perguntas recomendadas")
        List<String> sugestoes
) {
}
