package com.example.demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CorsConfigTest {

    @Test
    @DisplayName("Aceita só as origens configuradas")
    void aceitaOrigensConfiguradas() {
        CorsConfig config = new CorsConfig("https://app.exemplo.com, http://localhost:4200");
        assertArrayEquals(new String[]{"https://app.exemplo.com", "http://localhost:4200"}, config.origensPermitidas());
    }

    @Test
    @DisplayName("Ignora o curinga")
    void ignoraCuringa() {
        assertEquals(0, new CorsConfig("*").origensPermitidas().length);
        assertArrayEquals(new String[]{"https://app.exemplo.com"},
                new CorsConfig("*,https://app.exemplo.com").origensPermitidas());
    }

    @Test
    @DisplayName("Vazio não aceita origem externa")
    void vazioNaoAceitaOrigem() {
        assertEquals(0, new CorsConfig("").origensPermitidas().length);
        assertEquals(0, new CorsConfig(null).origensPermitidas().length);
    }
}
