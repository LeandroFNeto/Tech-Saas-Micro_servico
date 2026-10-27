package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MascaraLogTest {

    @Test
    @DisplayName("numero mantém só os 4 últimos dígitos de um JID @c.us")
    void numeroMantemQuatroUltimosDigitos() {
        assertThat(MascaraLog.numero("5511999997777@c.us")).isEqualTo("***7777");
    }

    @Test
    @DisplayName("numero sem sufixo também é mascarado")
    void numeroSemSufixo() {
        assertThat(MascaraLog.numero("5511999997777")).isEqualTo("***7777");
    }

    @Test
    @DisplayName("numero nunca devolve o número completo")
    void numeroNaoVazaCompleto() {
        assertThat(MascaraLog.numero("5511999997777@c.us")).doesNotContain("551199999");
    }

    @Test
    @DisplayName("numero com 4 dígitos ou menos devolve ***")
    void numeroCurto() {
        assertThat(MascaraLog.numero("1234@c.us")).isEqualTo("***");
        assertThat(MascaraLog.numero("12@c.us")).isEqualTo("***");
    }

    @Test
    @DisplayName("numero vazio ou nulo devolve ***")
    void numeroVazioENulo() {
        assertThat(MascaraLog.numero("")).isEqualTo("***");
        assertThat(MascaraLog.numero(null)).isEqualTo("***");
    }

    @Test
    @DisplayName("jid de número comum usa a máscara de número")
    void jidComum() {
        assertThat(MascaraLog.jid("5511999997777@c.us")).isEqualTo("***7777");
    }

    @Test
    @DisplayName("jid @lid devolve lid:*** com os 4 últimos caracteres")
    void jidLid() {
        String mascarado = MascaraLog.jid("276974622781686@lid");
        assertThat(mascarado).isEqualTo("lid:***1686");
        assertThat(mascarado).doesNotContain("27697462278");
    }

    @Test
    @DisplayName("jid @lid curto devolve lid:***")
    void jidLidCurto() {
        assertThat(MascaraLog.jid("123@lid")).isEqualTo("lid:***");
    }

    @Test
    @DisplayName("jid nulo ou vazio devolve ***")
    void jidNuloEVazio() {
        assertThat(MascaraLog.jid(null)).isEqualTo("***");
        assertThat(MascaraLog.jid("")).isEqualTo("***");
    }

    @Test
    @DisplayName("email mantém a primeira letra e o domínio")
    void emailMantemPrimeiraLetraEDominio() {
        String mascarado = MascaraLog.email("maria.silva@empresa.com");
        assertThat(mascarado).isEqualTo("m***@empresa.com");
        assertThat(mascarado).doesNotContain("maria.silva");
    }

    @Test
    @DisplayName("email sem @, com @ na posição 0, vazio ou nulo devolve ***")
    void emailInvalido() {
        assertThat(MascaraLog.email("semarroba")).isEqualTo("***");
        assertThat(MascaraLog.email("@dominio.com")).isEqualTo("***");
        assertThat(MascaraLog.email("")).isEqualTo("***");
        assertThat(MascaraLog.email(null)).isEqualTo("***");
    }

    @Test
    @DisplayName("tamanho informa só a quantidade de caracteres")
    void tamanhoInformaQuantidade() {
        String resultado = MascaraLog.tamanho("Olá, quero reservar");
        assertThat(resultado).isEqualTo("19 caracteres");
        assertThat(resultado).doesNotContain("reservar");
    }

    @Test
    @DisplayName("tamanho de nulo e de vazio é 0 caracteres")
    void tamanhoNuloEVazio() {
        assertThat(MascaraLog.tamanho(null)).isEqualTo("0 caracteres");
        assertThat(MascaraLog.tamanho("")).isEqualTo("0 caracteres");
    }
}
