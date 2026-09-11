package com.example.demo.dto.empresa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Contrato de atualização administrativa (PUT). Campos de infraestrutura e módulos; não deve ser usado pelo painel do cliente.")
public record EmpresaUpdateAdminDTO(
        @Schema(description = "Identificador da sessão que conecta com o WPPConnect", example = "sessao_recanto_01")
        String sessaoWhatsapp,

        @Schema(description = "Define se o local é alugado por hora (true) ou por diária (false)", example = "false")
        Boolean locacaoPorHora,

        @Schema(description = "ID da agenda Google desta empresa (não usar valor global no .env)",
                example = "abc123@group.calendar.google.com")
        String googleCalendarId,

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

        @Schema(description = "Lista de módulos liberados após pagamento", example = "[\"IA_GEMINI\", \"GOOGLE_CALENDAR\"]")
        List<String> modulosAtivos
) {
}
