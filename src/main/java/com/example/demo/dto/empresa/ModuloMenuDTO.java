package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item do menu do WhatsApp configurável pelo cliente. Não é a entidade JPA ModuloEmpresa.")
public record ModuloMenuDTO(
        @Schema(description = "Código da ação do bot (Fotos, Localização, Preços, Regras, Reserva)", example = "VER_FOTOS")
        String codigoAcao,

        @Schema(description = "Texto que aparece no menu do WhatsApp", example = "Ver fotos do espaço")
        String textoMenu,

        @Schema(description = "Se a opção aparece no menu do bot", example = "true")
        Boolean ativo,

        @Schema(description = "Ordem da opção no menu", example = "1")
        Integer ordemExibicao
) {
}
