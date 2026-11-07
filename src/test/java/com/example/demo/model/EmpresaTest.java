package com.example.demo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpresaTest {

    @Test
    @DisplayName("Valores padrão de Empresa recém-instanciada seguem contrato de negócio")
    void deveTerValoresPadraoConsistentes() {
        Empresa empresa = new Empresa();

        assertFalse(empresa.isLocacaoPorHora());
        assertFalse(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
        assertFalse(Boolean.TRUE.equals(empresa.getPermitePrevisaoTempo()));
        assertTrue(empresa.getAtivo());
        assertEquals(60, empresa.duracaoEfetivaMinutos());
        assertEquals(0, empresa.intervaloEfetivoMinutos());
        assertEquals(ModoLembrete.DESLIGADO, empresa.getLembreteModo());
        assertEquals(1, empresa.getLembreteDiasAntes());
        assertEquals("10:00", empresa.getLembreteHorarioEnvio());
    }

    @Test
    @DisplayName("Setters e valores efetivos de Empresa preservam configuração e fallbacks")
    void deveRespeitarFallbacksESettersDeEmpresa() {
        Empresa empresa = new Empresa();
        empresa.setId(9L);
        empresa.setNome("Recanto Azul");
        empresa.setSessaoWhatsapp("recanto_azul");
        empresa.setRamoDeAtuacao("LOCACAO");
        empresa.setDuracaoAtendimentoMinutos(90);
        empresa.setIntervaloEntreAtendimentosMinutos(15);
        empresa.setPrevisaoTempoAtiva(true);
        empresa.setPermitePrevisaoTempo(null);
        empresa.setAtivo(null);
        empresa.setLembreteModo(null);
        empresa.setLembreteDiasAntes(null);
        empresa.setLembreteHorarioEnvio(null);

        assertEquals(9L, empresa.getId());
        assertEquals("Recanto Azul", empresa.getNome());
        assertEquals("recanto_azul", empresa.getSessaoWhatsapp());
        assertEquals("LOCACAO", empresa.getRamoDeAtuacao());
        assertEquals(90, empresa.duracaoEfetivaMinutos());
        assertEquals(15, empresa.intervaloEfetivoMinutos());
        assertTrue(empresa.getPermitePrevisaoTempo());
        assertTrue(empresa.getAtivo());
        assertEquals(ModoLembrete.DESLIGADO, empresa.getLembreteModo());
        assertEquals(1, empresa.getLembreteDiasAntes());
        assertEquals("10:00", empresa.getLembreteHorarioEnvio());

        empresa.setPermitePrevisaoTempo(false);
        empresa.setAtivo(false);
        assertFalse(empresa.getPermitePrevisaoTempo());
        assertFalse(empresa.getAtivo());
    }

    @Test
    @DisplayName("setUrlsGaleria filtra vazios, aplica trim e limita em 5 URLs")
    void deveNormalizarELimitarUrlsDaGaleria() {
        Empresa empresa = new Empresa();
        empresa.setUrlsGaleria(Arrays.asList(
                " https://img/1.jpg ",
                "",
                null,
                "https://img/2.jpg",
                "https://img/3.jpg",
                "https://img/4.jpg",
                "https://img/5.jpg",
                "https://img/6.jpg"
        ));

        assertEquals(List.of(
                "https://img/1.jpg",
                "https://img/2.jpg",
                "https://img/3.jpg",
                "https://img/4.jpg",
                "https://img/5.jpg"
        ), empresa.getUrlsGaleria());

        empresa.setUrlsGaleria(null);
        assertTrue(empresa.getUrlsGaleria().isEmpty());
    }

    @Test
    @DisplayName("setEmailsResponsaveisReuniao remove brancos, aplica trim e elimina duplicatas")
    void deveNormalizarEmailsDosResponsaveis() {
        Empresa empresa = new Empresa();
        empresa.setEmailsResponsaveisReuniao(Arrays.asList(
                " ana@gamb.com.br ",
                "  ",
                null,
                "ana@gamb.com.br",
                "leo@gamb.com.br"
        ));

        assertEquals(List.of("ana@gamb.com.br", "leo@gamb.com.br"), empresa.getEmailsResponsaveisReuniao());
    }

    @Test
    @DisplayName("DadosReservaSessao ordena datas, impede duplicatas, atualiza data principal e respeita limite de 10")
    void deveGerenciarDatasEmDadosReservaSessao() {
        DadosReservaSessao dados = new DadosReservaSessao();
        LocalDate d2 = LocalDate.of(2026, 12, 20);
        LocalDate d1 = LocalDate.of(2026, 12, 10);

        assertFalse(dados.adicionarData(null));
        assertTrue(dados.adicionarData(d2));
        assertTrue(dados.adicionarData(d1));
        assertFalse(dados.adicionarData(d1));

        assertEquals(List.of(d1, d2), dados.getDatas());
        assertEquals(d1, dados.getData());
        assertThrows(UnsupportedOperationException.class, () -> dados.getDatas().add(LocalDate.now()));

        assertTrue(dados.removerData(d1));
        assertEquals(d2, dados.getData());
        assertTrue(dados.removerData(d2));
        assertNull(dados.getData());

        for (int i = 1; i <= DadosReservaSessao.MAXIMO_DATAS; i++) {
            assertFalse(dados.atingiuLimiteDeDatas());
            assertTrue(dados.adicionarData(LocalDate.of(2026, 11, i)));
        }
        assertTrue(dados.atingiuLimiteDeDatas());
    }
}
