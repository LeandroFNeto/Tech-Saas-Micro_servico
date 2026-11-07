package com.example.demo.dto.empresa;

import java.time.LocalDateTime;
import java.util.List;

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
        String linkGaleria,
        String googleCalendarId,
        Boolean locacaoPorHora,
        Boolean permiteReservaAutomatica,
        Boolean permitePrevisaoTempo,
        List<ModuloPainelDTO> modulosAtivos,
        List<String> emailsResponsaveisReuniao,
        String cidadePrevisao,
        String cidadePrevisaoResolvida,
        Boolean previsaoTempoAtiva,
        LembreteConfirmacaoDTO lembreteConfirmacao,
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
