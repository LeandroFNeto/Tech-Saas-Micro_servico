package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.ModuloEmpresa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicoMenuTest {

    private final ServicoMenu servicoMenu = new ServicoMenu();

    private ModuloEmpresa modulo(String codigo, String texto, Integer ordem, boolean ativo) {
        ModuloEmpresa m = new ModuloEmpresa();
        m.setCodigoAcao(codigo);
        m.setTextoMenu(texto);
        m.setOrdemExibicao(ordem);
        m.setAtivo(ativo);
        return m;
    }

    @Test
    @DisplayName("montarMenuPrincipal usa saudação padrão quando vazia e ordena módulos ativos com nullsLast")
    void deveMontarMenuOrdenadoComSaudacaoPadrao() {
        Empresa empresa = new Empresa();
        empresa.setMensagemSaudacao("  ");
        empresa.setModulosAtivos(List.of(
                modulo("SOLICITAR_RESERVA", "Reservar", null, true),
                modulo("VER_REGRAS", "Regras", 2, false),
                modulo("VER_FOTOS", "Fotos", 1, true)
        ));

        String menu = servicoMenu.montarMenuPrincipal(empresa);

        assertTrue(menu.startsWith("Olá!\n\n"));
        assertTrue(menu.contains("1️⃣ - Fotos\n2️⃣ - Reservar\n"));
        assertEquals("VER_FOTOS", servicoMenu.descobrirAcao(empresa, " 1 "));
        assertEquals("SOLICITAR_RESERVA", servicoMenu.descobrirAcao(empresa, "2"));
    }

    @Test
    @DisplayName("descobrirAcao retorna OPCAO_INVALIDA para zero, fora da faixa, texto inválido ou nulo")
    void deveRetornarOpcaoInvalidaParaEntradasInvalidas() {
        Empresa empresa = new Empresa();
        empresa.setMensagemSaudacao("Bem-vindo!");
        empresa.setModulosAtivos(List.of(modulo("VER_FOTOS", "Fotos", 1, true)));

        assertTrue(servicoMenu.montarMenuPrincipal(empresa).startsWith("Bem-vindo!"));
        assertEquals("OPCAO_INVALIDA", servicoMenu.descobrirAcao(empresa, "0"));
        assertEquals("OPCAO_INVALIDA", servicoMenu.descobrirAcao(empresa, "2"));
        assertEquals("OPCAO_INVALIDA", servicoMenu.descobrirAcao(empresa, "abc"));
        assertEquals("OPCAO_INVALIDA", servicoMenu.descobrirAcao(empresa, null));
        assertTrue(servicoMenu.listarAtivosOrdenados(null).isEmpty());
    }
}
