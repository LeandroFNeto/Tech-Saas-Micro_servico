package com.example.demo.servico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InterpretadorPorPalavrasTest {

    private InterpretadorPorPalavras interpretador;

    @BeforeEach
    void setUp() {
        interpretador = new InterpretadorPorPalavras();
    }

    @Test
    @DisplayName("Reconhece confirmações afirmativas")
    void deveReconhecerConfirmacao() {
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Sim, confirmo"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Pode ser"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Tudo certo, estarei lá"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("OK"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Vou sim"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("👍"));
    }

    @Test
    @DisplayName("Expressões negadas jamais retornam CONFIRMA")
    void naoDeveClassificarNegacoesComoConfirmacao() {
        assertNotEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Não confirmo"));
        assertNotEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("nao confirmo"));
        assertNotEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Infelizmente não está confirmado"));
        assertNotEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Não está ok"));
        assertNotEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("nao esta ok"));
    }

    @Test
    @DisplayName("Reconhece cancelamento ou remarcação")
    void deveReconhecerCancelamentoOuRemarcacao() {
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Não posso ir"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Preciso cancelar"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Gostaria de mudar a data"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Remarcar"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Desmarcar reserva"));
    }

    @Test
    @DisplayName("Reconhece pedido de parar notificações")
    void deveReconhecerParar() {
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("parar"));
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("pare"));
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("stop"));
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("parar avisos"));
    }

    @Test
    @DisplayName("Retorna OUTRO quando mensagem não corresponde a nenhuma intenção")
    void deveReconhecerOutroQuandoMensagemNaoCorresponde() {
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar("Quanto custa o serviço?"));
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar("Onde fica o local?"));
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar(""));
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar(null));
    }
}
