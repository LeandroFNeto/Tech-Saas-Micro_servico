package com.example.demo.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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

    @Test
    @DisplayName("Chave de criptografia so abre o valor da propria chave e a validacao recusa chave curta")
    void deveAbrirSoComAChaveCerta() {
        String cifrado = CriptografiaCampo.criptografar("5511999998888");
        String outra = Base64.getEncoder().encodeToString("abcdefghijklmnopqrstuvwxyz012345".getBytes());

        assertEquals("5511999998888", CriptografiaCampo.tentarDescriptografarCom(cifrado, chave32BytesValida));
        assertNull(CriptografiaCampo.tentarDescriptografarCom(cifrado, outra));
        assertNull(CriptografiaCampo.tentarDescriptografarCom("sem-prefixo", chave32BytesValida));
        assertTrue(CriptografiaCampo.chaveCriptoValida(outra));
        assertFalse(CriptografiaCampo.chaveCriptoValida("curta"));
        assertFalse(CriptografiaCampo.chaveCriptoValida(null));
    }

    @Test
    @DisplayName("Sem chave configurada, o conversor nao assume valor escrito no codigo")
    void naoDeveTerChavePadrao() {
        new CriptografiaCampo("", "");
        assertFalse(CriptografiaCampo.isConfigurado());
        assertThrows(IllegalStateException.class, () -> CriptografiaCampo.criptografar("x"));
    }

    @Test
    @DisplayName("regressao: o Spring carrega as chaves das propriedades ao criar o componente")
    void springCarregaAsChavesDasPropriedades() {
        CriptografiaCampo.configurarChaves(null, null);
        String outraChave = Base64.getEncoder().encodeToString("abcdefghijabcdefghijabcdefghij12".getBytes());

        new ApplicationContextRunner()
                .withPropertyValues("contatos.cripto.chave=" + outraChave, "contatos.indice.chave=indice-vindo-do-spring")
                .withUserConfiguration(CriptografiaCampo.class)
                .run(contexto -> {
                    assertNotNull(contexto.getBean(CriptografiaCampo.class));
                    assertTrue(CriptografiaCampo.isConfigurado());
                    String cifrado = CriptografiaCampo.criptografar("5511999998888");
                    assertEquals("5511999998888", CriptografiaCampo.tentarDescriptografarCom(cifrado, outraChave));
                });
    }
}
