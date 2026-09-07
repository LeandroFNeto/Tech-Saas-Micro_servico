package com.example.demo.dto.aviso;

import com.example.demo.model.TipoAviso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Contrato de aviso do painel (manutenção, tutorial, preço ou comunicado). Não expõe a entidade JPA.")
public record AvisoDTO(
        @Schema(description = "Identificador único do aviso. Omitido na criação; o servidor gera o valor.", example = "1")
        Long id,

        @NotNull
        @Schema(description = "Categoria do aviso, usada pelo painel do cliente para escolher o layout", example = "TUTORIAL")
        TipoAviso tipo,

        @NotBlank
        @Schema(description = "Título exibido no banner ou no card", example = "Como conectar o WhatsApp")
        String titulo,

        @NotBlank
        @Schema(description = "Corpo do aviso em texto ou HTML sanitizado no frontend",
                example = "Abra a sessão, escaneie o QR Code e volte aqui para conferir o status.")
        String conteudo,

        @Schema(description = "URL opcional (tutorial no YouTube, tabela de preços, etc.)",
                example = "https://www.youtube.com/watch?v=exemplo")
        String link,

        @Schema(description = "Se false, o aviso some do GET público e permanece visível só no admin", example = "true")
        Boolean ativo
) {
}
