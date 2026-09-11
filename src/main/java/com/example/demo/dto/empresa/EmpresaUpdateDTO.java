package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Contrato de atualização do painel do cliente (PUT). Não inclui campos de cobrança ou infraestrutura.")
public record EmpresaUpdateDTO(
        @Schema(description = "Nome fantasia", example = "Recanto Vista Alegre")
        String nome,

        @Schema(description = "Mensagem que o bot envia ao iniciar a conversa", example = "Olá! Como posso ajudar?")
        String mensagemSaudacao,

        @Schema(description = "Mensagem de preços que o bot envia no WhatsApp quando o cliente pede a tabela",
                example = "Diária padrão: R$ 500,00")
        String tabelaDePrecos,

        @Schema(description = "Link do Google Maps do estabelecimento", example = "https://maps.app.goo.gl/exemplo")
        String linkGoogleMaps,

        @Schema(description = "URL pública da foto principal (Hero Image) hospedada no Cloudinary",
                example = "https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg")
        String linkFotoPrincipal,

        @Schema(description = "Lista de URLs públicas das fotos complementares hospedadas no Cloudinary",
                example = "[\"https://res.cloudinary.com/demo/image/upload/v1/foto1.jpg\"]")
        @Size(max = 5)
        List<String> urlsGaleria,

        @Schema(description = "Link opcional da galeria completa no Google Drive (fotos e vídeos em alta qualidade)",
                example = "https://drive.google.com/drive/folders/exemplo")
        String linkGaleria,

        @Schema(description = "Ativa ou desativa a reserva automática pelo bot no WhatsApp", example = "false")
        Boolean permiteReservaAutomatica,

        @Schema(description = "Define se o local é alugado por hora (true) ou por diária (false)", example = "false")
        Boolean locacaoPorHora,

        @Schema(description = "Regras do local e política de cancelamento lidas pelo bot",
                example = "Cancelar com 5 dias de antecedência")
        String regrasLocacao,

        @Schema(description = "Opções do menu do WhatsApp (Fotos, Localização, Preços, Regras, Reserva)")
        List<ModuloMenuDTO> modulosMenu
) {
}
