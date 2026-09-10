package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Contrato de saída da API de empresas. Não expõe a entidade JPA nem campos internos de infraestrutura.")
public record EmpresaResponseDTO(
        @Schema(description = "Identificador único da empresa", example = "1")
        Long id,

        @Schema(description = "Nome fantasia", example = "Recanto Vista Alegre")
        String nome,

        @Schema(description = "Identificador da sessão no WPPConnect, usado pelo front-end para acompanhar a conexão", example = "sessao_recanto_01")
        String sessaoWhatsapp,

        @Schema(description = "Status atual da sessão WhatsApp", example = "DISCONNECTED")
        String statusSessao,

        @Schema(description = "Link configurado para o Google Maps", example = "https://maps.app.goo.gl/exemplo")
        String linkGoogleMaps,

        @Schema(description = "URL pública da foto principal (Hero Image) hospedada no Cloudinary",
                example = "https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg")
        String linkFotoPrincipal,

        @Schema(description = "Lista de URLs públicas das fotos complementares hospedadas no Cloudinary")
        List<String> urlsGaleria,

        @Schema(description = "Quando true, o bot exibe a opção de reserva automática no WhatsApp", example = "false")
        Boolean permiteReservaAutomatica,

        @Schema(description = "Define se o local é alugado por hora (true) ou por diária (false)", example = "false")
        Boolean locacaoPorHora,

        @Schema(description = "Regras do local e política de cancelamento lidas pelo bot",
                example = "Cancelar com 5 dias de antecedência")
        String regrasLocacao,

        @Schema(description = "Opções do menu do WhatsApp configuradas pelo cliente")
        List<ModuloMenuDTO> modulosMenu,

        @Schema(description = "Data e hora da última modificação retornada pela API", example = "2026-09-03T14:30:00")
        LocalDateTime atualizadoEm
) {
}
