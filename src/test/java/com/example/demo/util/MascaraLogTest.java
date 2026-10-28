package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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

    @Test
    @DisplayName("idMensagem nulo ou vazio devolve ***")
    void idMensagemNuloEVazio() {
        assertThat(MascaraLog.idMensagem(null)).isEqualTo("***");
        assertThat(MascaraLog.idMensagem("")).isEqualTo("***");
    }

    @Test
    @DisplayName("idMensagem sem @ volta intacto")
    void idMensagemSemArroba() {
        assertThat(MascaraLog.idMensagem("ABCDEF123456")).isEqualTo("ABCDEF123456");
    }

    @Test
    @DisplayName("idMensagem com JID mascara só o telefone e mantém prefixo, domínio e hash")
    void idMensagemComJid() {
        String mascarado = MascaraLog.idMensagem("false_5511999997777@c.us_3AE7951B92607B7B73F8");
        assertThat(mascarado).isEqualTo("false_***7777@c.us_3AE7951B92607B7B73F8");
        assertThat(mascarado).doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("idMensagem mantém o prefixo true_")
    void idMensagemPrefixoTrue() {
        assertThat(MascaraLog.idMensagem("true_5511999997777@c.us_ABC123"))
                .isEqualTo("true_***7777@c.us_ABC123");
    }

    @Test
    @DisplayName("idMensagem com @lid vira lid:*** com os 4 últimos caracteres")
    void idMensagemLid() {
        String mascarado = MascaraLog.idMensagem("false_276974622781686@lid_HASH99");
        assertThat(mascarado).isEqualTo("false_lid:***1686@lid_HASH99");
        assertThat(mascarado).doesNotContain("27697462278");
    }

    @Test
    @DisplayName("idMensagem com parte de 4 dígitos ou menos vira ***")
    void idMensagemParteCurta() {
        assertThat(MascaraLog.idMensagem("false_1234@c.us_HASH")).isEqualTo("false_***@c.us_HASH");
        assertThat(MascaraLog.idMensagem("false_123@lid_HASH")).isEqualTo("false_lid:***@lid_HASH");
    }

    @Test
    @DisplayName("idMensagem de grupo @g.us com hífens mascara só os dígitos")
    void idMensagemGrupo() {
        String mascarado = MascaraLog.idMensagem("false_5511999997777-1600000000@g.us_HASH");
        assertThat(mascarado).isEqualTo("false_***0000@g.us_HASH");
        assertThat(mascarado).doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("idMensagem com JID sem hash final mascara o telefone")
    void idMensagemSemHash() {
        assertThat(MascaraLog.idMensagem("false_5511999997777@c.us")).isEqualTo("false_***7777@c.us");
    }

    @Test
    @DisplayName("idMensagem de mensagem direta com hash mascara só o telefone")
    void idMensagemDiretaComHash() {
        String mascarado = MascaraLog.idMensagem("false_5511999997777@c.us_3AE7951B92607B7B73F8");
        assertThat(mascarado).isEqualTo("false_***7777@c.us_3AE7951B92607B7B73F8");
        assertThat(mascarado).doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("idMensagem de grupo mascara o grupo e o participante")
    void idMensagemGrupoComParticipante() {
        String mascarado = MascaraLog.idMensagem("false_120363012345678901@g.us_3EB0ABC_5511999997777@c.us");
        assertThat(mascarado).isEqualTo("false_***8901@g.us_3EB0ABC_***7777@c.us");
        assertThat(mascarado).doesNotContain("5511999997777").doesNotContain("120363012345678901");
    }

    @Test
    @DisplayName("idMensagem só com dígitos é mascarado")
    void idMensagemSoDigitos() {
        String mascarado = MascaraLog.idMensagem("5511999997777");
        assertThat(mascarado).isEqualTo("***7777");
        assertThat(mascarado).doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("idMensagem sem @ com segmento numérico mascara o número")
    void idMensagemSemArrobaComSegmentoNumerico() {
        String mascarado = MascaraLog.idMensagem("true_5511999997777_ABC");
        assertThat(mascarado).isEqualTo("true_***7777_ABC");
        assertThat(mascarado).doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("idMensagem com @lid de 11 caracteres mantém os 4 últimos")
    void idMensagemLidLongo() {
        String mascarado = MascaraLog.idMensagem("false_ABCD1234567@lid_HASH");
        assertThat(mascarado).isEqualTo("false_lid:***4567@lid_HASH");
        assertThat(mascarado).doesNotContain("ABCD1234567");
    }

    @Test
    @DisplayName("idMensagem com entradas de borda não lança exceção")
    void idMensagemBordas() {
        assertThat(MascaraLog.idMensagem("@")).isEqualTo("***@");
        assertThat(MascaraLog.idMensagem("x@@c.us")).isEqualTo("***@@c.us");
        assertThat(MascaraLog.idMensagem("_@")).isEqualTo("_***@");
        assertThat(MascaraLog.idMensagem("___")).isEqualTo("___");
        assertThat(MascaraLog.idMensagem("1234")).isEqualTo("1234");
        assertThat(MascaraLog.idMensagem("123-456@g.us")).isEqualTo("***3456@g.us");
    }

    @Test
    @DisplayName("idMensagem com participante @lid dentro de grupo mascara os dois")
    void idMensagemParticipanteLidEmGrupo() {
        String mascarado = MascaraLog.idMensagem("false_1203@g.us_HASH_ABCD1234567@lid");
        assertThat(mascarado).isEqualTo("false_***@g.us_HASH_lid:***4567@lid");
        assertThat(mascarado).doesNotContain("ABCD1234567").doesNotContain("1203@");
    }

    @Test
    @DisplayName("idMensagem gigante é mascarado sem exceção e em tempo curto")
    void idMensagemGigante() {
        String gigante = "a".repeat(100000) + "@c.us";
        String mascarado = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> MascaraLog.idMensagem(gigante));
        assertThat(mascarado).isEqualTo("***@c.us");
    }
}
