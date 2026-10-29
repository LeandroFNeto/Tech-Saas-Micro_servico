package com.example.demo.servico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterpretadorPorPalavrasTest {

    private InterpretadorPorPalavras interpretador;

    @BeforeEach
    void setUp() {
        interpretador = new InterpretadorPorPalavras();
    }

    @Test
    void deveReconhecerConfirmacao() {
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Sim, confirmo"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Pode ser"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Tudo certo, estarei lá"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("OK"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("Vou sim"));
        assertEquals(IntencaoResposta.CONFIRMA, interpretador.interpretar("👍"));
    }

    @Test
    void deveReconhecerCancelamentoOuRemarcacao() {
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Não posso ir"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Preciso cancelar"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Gostaria de mudar a data"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Remarcar"));
        assertEquals(IntencaoResposta.REMARCA, interpretador.interpretar("Desmarcar reserva"));
    }

    @Test
    void deveReconhecerParar() {
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("parar"));
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("pare"));
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("stop"));
        assertEquals(IntencaoResposta.PARAR, interpretador.interpretar("parar avisos"));
    }

    @Test
    void deveReconhecerOutroQuandoMensagemNaoCorresponde() {
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar("Quanto custa o serviço?"));
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar("Onde fica o local?"));
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar(""));
        assertEquals(IntencaoResposta.OUTRO, interpretador.interpretar(null));
    }
}
