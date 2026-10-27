package com.example.demo.servico.observador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ObservadorwhatsappTest {

    private static final String JID = "5511999997777@c.us";
    private static final String NUMERO_COMPLETO = "5511999997777";
    private static final String TEXTO = "Quero reservar o salao para o aniversario da Joana";

    private final Observadorwhatsapp observador = new Observadorwhatsapp();

    @Test
    @DisplayName("logSucesso não escreve o texto nem o número completo")
    void sucessoNaoVazaTextoNemNumero(CapturedOutput saida) {
        observador.logSucesso("sessao_recanto_01", JID, TEXTO);

        assertThat(saida.getAll()).doesNotContain(TEXTO).doesNotContain("aniversario").doesNotContain(NUMERO_COMPLETO);
    }

    @Test
    @DisplayName("logSucesso escreve a sessão, o número mascarado e o tamanho")
    void sucessoEscreveMascaraETamanho(CapturedOutput saida) {
        observador.logSucesso("sessao_recanto_01", JID, TEXTO);

        assertThat(saida.getAll())
                .contains("sessao_recanto_01")
                .contains("***7777")
                .contains(TEXTO.length() + " caracteres");
    }

    @Test
    @DisplayName("logFiltro mascara o número e mantém o motivo")
    void filtroMascaraNumero(CapturedOutput saida) {
        observador.logFiltro("sessao_recanto_01", JID, "Dono do celular digitou (Anti-X9)");

        assertThat(saida.getAll())
                .contains("***7777")
                .contains("Anti-X9")
                .doesNotContain(NUMERO_COMPLETO);
    }

    @Test
    @DisplayName("logFiltro com jid @lid usa a máscara de lid")
    void filtroMascaraLid(CapturedOutput saida) {
        observador.logFiltro("sessao_recanto_01", "276974622781686@lid", "ignorada");

        assertThat(saida.getAll()).contains("lid:***1686").doesNotContain("276974622781686");
    }

    @Test
    @DisplayName("logErro não escreve a mensagem da exceção e escreve a classe")
    void erroNaoEscreveMensagem(CapturedOutput saida) {
        Exception e = new IllegalStateException("falha com o numero " + NUMERO_COMPLETO + " texto " + TEXTO);

        observador.logErro("sessao_recanto_01", JID, "ControllerWhatsapp", e);

        assertThat(saida.getAll())
                .contains("IllegalStateException")
                .contains("ControllerWhatsapp")
                .contains("***7777")
                .doesNotContain(NUMERO_COMPLETO)
                .doesNotContain(TEXTO)
                .doesNotContain("falha com o numero");
    }

    @Test
    @DisplayName("logErro de cliente HTTP informa o código e não o corpo da resposta")
    void erroHttpInformaCodigoSemCorpo(CapturedOutput saida) {
        Exception e = new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "{\"cliente\":\"Joana Prado\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        observador.logErro("sessao_recanto_01", JID, "ServicoMensagem", e);

        assertThat(saida.getAll())
                .contains("HttpClientErrorException")
                .contains("HTTP 401")
                .doesNotContain("Joana Prado")
                .doesNotContain(NUMERO_COMPLETO);
    }
}
