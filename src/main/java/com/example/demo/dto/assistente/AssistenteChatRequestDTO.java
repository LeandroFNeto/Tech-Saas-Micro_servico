package com.example.demo.dto.assistente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pergunta ou comando enviado ao assistente interno de tutoria e diagnóstico")
public record AssistenteChatRequestDTO(
        @NotBlank
        @Schema(description = "Texto com a dúvida operacional, pedido de ajuda de configuração ou comando de teste", example = "Como faço para cadastrar um novo aviso no painel?")
        String mensagem,

        @Schema(description = "Modo de atuação selecionado no painel administrativo", example = "DIAGNOSTICO")
        String modo
) {
}
