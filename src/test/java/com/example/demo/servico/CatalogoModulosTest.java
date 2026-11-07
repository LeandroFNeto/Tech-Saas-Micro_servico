package com.example.demo.servico;

import com.example.demo.model.Empresa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoModulosTest {

    @Test
    @DisplayName("aplicar substitui módulos, atribui ordem e textos padrão e sincroniza usaIA")
    void deveAplicarCatalogoESincronizarUsaIA() {
        Empresa empresa = new Empresa();
        CatalogoModulos.aplicar(empresa, Arrays.asList(" IA_GEMINI ", "", null, "VER_FOTOS"));

        assertTrue(empresa.getUsaIA());
        assertEquals(2, empresa.getModulosAtivos().size());
        assertEquals("IA_GEMINI", empresa.getModulosAtivos().get(0).getCodigoAcao());
        assertEquals("Falar com atendente virtual", empresa.getModulosAtivos().get(0).getTextoMenu());
        assertEquals(1, empresa.getModulosAtivos().get(0).getOrdemExibicao());
        assertEquals("VER_FOTOS", empresa.getModulosAtivos().get(1).getCodigoAcao());
        assertEquals(2, empresa.getModulosAtivos().get(1).getOrdemExibicao());

        CatalogoModulos.aplicar(empresa, List.of("VER_REGRAS"));
        assertFalse(empresa.getUsaIA());
        assertEquals(1, empresa.getModulosAtivos().size());

        CatalogoModulos.aplicar(empresa, null);
        assertEquals(1, empresa.getModulosAtivos().size());
    }

    @Test
    @DisplayName("upsertItemMenu insere, atualiza texto e ativo, aplica fallback de texto e ignora códigos fora do menu")
    void deveFazerUpsertApenasDeCodigosDoMenuDoBot() {
        Empresa empresa = new Empresa();
        empresa.setModulosAtivos(null);

        assertFalse(CatalogoModulos.isCodigoMenuBot("IA_GEMINI"));
        assertTrue(CatalogoModulos.isCodigoMenuBot(" VER_FOTOS "));
        assertEquals("", CatalogoModulos.textoPadrao(null));
        assertEquals("CUSTOM", CatalogoModulos.textoPadrao("CUSTOM"));

        CatalogoModulos.upsertItemMenu(empresa, "IA_GEMINI", "Ignorar", true);
        assertNull(empresa.getModulosAtivos());

        CatalogoModulos.upsertItemMenu(empresa, "VER_FOTOS", "  ", null);
        assertEquals(1, empresa.getModulosAtivos().size());
        assertEquals("Ver fotos do espaço", empresa.getModulosAtivos().get(0).getTextoMenu());
        assertTrue(empresa.getModulosAtivos().get(0).getAtivo());

        CatalogoModulos.upsertItemMenu(empresa, "VER_FOTOS", "Galeria do sítio", false);
        assertEquals(1, empresa.getModulosAtivos().size());
        assertEquals("Galeria do sítio", empresa.getModulosAtivos().get(0).getTextoMenu());
        assertFalse(empresa.getModulosAtivos().get(0).getAtivo());
    }
}
