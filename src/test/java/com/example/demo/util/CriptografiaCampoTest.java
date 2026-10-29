package com.example.demo.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CriptografiaCampoTest {

    private final String chave32BytesValida = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());
    private final String chaveIndiceValida = "minha-chave-indice-teste-32bytes";

    @BeforeEach
    void setUp() {
        CriptografiaCampo.configurarChaves(chave32BytesValida, chaveIndiceValida);
        CriptografiaCampo.resetarAviso();
    }

    @AfterEach
    void tearDown() {
        CriptografiaCampo.configurarChaves(chave32BytesValida, chaveIndiceValida);
        CriptografiaCampo.resetarAviso();
    }

    @Test
    void deveCriptografarEDescriptografarComSucesso() {
        String textoOriginal = "5511999998888";
        String cifrado = CriptografiaCampo.criptografar(textoOriginal);

        assertNotNull(cifrado);
        assertTrue(cifrado.startsWith("v1:"));
        assertNotEquals(textoOriginal, cifrado);

        String decifrado = CriptografiaCampo.descriptografar(cifrado);
        assertEquals(textoOriginal, decifrado);
    }

    @Test
    void deveRetornarNuloParaEntradaNula() {
        assertNull(CriptografiaCampo.criptografar(null));
        assertNull(CriptografiaCampo.descriptografar(null));
        assertNull(CriptografiaCampo.calcularIndice(null));
    }

    @Test
    void deveGerarIndiceDeterministoEHashConsistente() {
        String texto = "5511999998888";
        String indice1 = CriptografiaCampo.calcularIndice(texto);
        String indice2 = CriptografiaCampo.calcularIndice(texto);

        assertNotNull(indice1);
        assertEquals(indice1, indice2);
        assertNotEquals(texto, indice1);
    }

    @Test
    void deveFalharAoCriptografarQuandoChavesEstaoDesconfiguradas() {
        CriptografiaCampo.configurarChaves(null, null);
        assertFalse(CriptografiaCampo.isConfigurado());

        assertThrows(IllegalStateException.class, () -> CriptografiaCampo.criptografar("teste"));
        assertThrows(IllegalStateException.class, () -> CriptografiaCampo.calcularIndice("teste"));
    }

    @Test
    void deveRetornarProprioTextoSeNaoPossuiPrefixoVersaoAoDescriptografar() {
        String textoLegado = "texto_sem_prefixo";
        assertEquals(textoLegado, CriptografiaCampo.descriptografar(textoLegado));
    }
}
