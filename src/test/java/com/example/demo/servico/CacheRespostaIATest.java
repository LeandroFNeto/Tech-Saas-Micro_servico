package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CacheRespostaIATest {

    @Test
    @DisplayName("Reaproveita a resposta dentro da validade e descarta depois")
    void respeitaValidade() {
        Instant inicio = Instant.parse("2026-11-07T15:00:00Z");
        CacheRespostaIA cache = new CacheRespostaIA(10, Clock.fixed(inicio, ZoneOffset.UTC));
        cache.guardar("pergunta", "resposta");

        assertThat(cache.obter("pergunta")).contains("resposta");
        assertThat(cache.obter("outra")).isEmpty();

        CacheRespostaIA expirado = new CacheRespostaIA(10, Clock.fixed(inicio, ZoneOffset.UTC));
        expirado.guardar("pergunta", "resposta");
        assertThat(new CacheRespostaIA(0, Clock.fixed(inicio, ZoneOffset.UTC)).obter("pergunta")).isEmpty();
    }

    @Test
    @DisplayName("Não guarda resposta vazia")
    void naoGuardaVazia() {
        CacheRespostaIA cache = new CacheRespostaIA(10, Clock.systemUTC());
        cache.guardar("p", " ");

        assertThat(cache.obter("p")).isEmpty();
    }
}
