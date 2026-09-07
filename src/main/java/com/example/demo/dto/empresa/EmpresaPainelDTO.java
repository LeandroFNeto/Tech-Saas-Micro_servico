package com.example.demo.dto.empresa;

import java.time.LocalDateTime;
import java.util.List;

/** Visão HAL da empresa para o painel (GET). Não é a entidade JPA. */
public record EmpresaPainelDTO(
        Long id,
        String nome,
        Boolean usaIA,
        String sessaoWhatsapp,
        String statusSessao,
        String mensagemSaudacao,
        String ramoDeAtuacao,
        String tabelaDePrecos,
        String linkGoogleMaps,
        String linkFotoPrincipal,
        String linkGaleria,
        String googleCalendarId,
        Boolean locacaoPorHora,
        List<ModuloPainelDTO> modulosAtivos,
        LocalDateTime atualizadoEm
) {
    public record ModuloPainelDTO(
            String codigoAcao,
            String textoMenu,
            Integer ordemExibicao,
            Boolean ativo
    ) {
    }
}
