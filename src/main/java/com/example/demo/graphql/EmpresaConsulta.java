package com.example.demo.graphql;

import java.util.List;

/**
 * Projeção da empresa para a camada GraphQL da IA.
 * Não é a entidade JPA e não carrega usuário, senha nem calendário.
 */
public record EmpresaConsulta(
        String id,
        String nome,
        String tabelaDePrecos,
        String ramoDeAtuacao,
        String regrasLocacao,
        String mensagemSaudacao,
        String sessaoWhatsapp,
        String linkGoogleMaps,
        String linkFotoPrincipal,
        String linkGaleria,
        List<String> urlsGaleria,
        Boolean usaIA,
        Boolean locacaoPorHora,
        Boolean permiteReservaAutomatica
) {
}
