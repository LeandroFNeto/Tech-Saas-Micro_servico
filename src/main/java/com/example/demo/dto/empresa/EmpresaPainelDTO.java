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
        String regrasLocacao,
        String linkGoogleMaps,
        String linkFotoPrincipal,
        List<String> urlsGaleria,
        String googleCalendarId,
        Boolean locacaoPorHora,
        Boolean permiteReservaAutomatica,
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
