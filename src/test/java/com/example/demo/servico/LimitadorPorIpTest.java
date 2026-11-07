package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorPorIpTest {

    private static final class RelogioMutavel extends Clock {
        private final AtomicReference<Instant> instante;

        RelogioMutavel(Instant inicial) {
            this.instante = new AtomicReference<>(inicial);
        }

        void avancar(Duration d) {
            instante.updateAndGet(atual -> atual.plus(d));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    @Test
    @DisplayName("Permite até 10 requisições por minuto no mesmo IP, bloqueia a 11ª e libera após a janela")
    void deveRespeitarLimitePorJanelaDeslizante() {
        RelogioMutavel relogio = new RelogioMutavel(Instant.parse("2026-11-07T12:00:00Z"));
        LimitadorPorIp limitador = new LimitadorPorIp(relogio);

        for (int i = 0; i < 10; i++) {
            assertTrue(limitador.permitir("192.168.0.10"));
        }
        assertFalse(limitador.permitir("192.168.0.10"));
        assertTrue(limitador.permitir("192.168.0.11"));

        relogio.avancar(Duration.ofSeconds(61));
        assertTrue(limitador.permitir("192.168.0.10"));
    }

    @Test
    @DisplayName("Trata IP nulo como chave 'desconhecido' sem lançar exceção")
    void deveTratarIpNuloComSeguranca() {
        LimitadorPorIp limitador = new LimitadorPorIp();
        for (int i = 0; i < 10; i++) {
            assertTrue(limitador.permitir(null));
        }
        assertFalse(limitador.permitir(null));
    }

    @Test
    @DisplayName("Suporta carga concorrente acima de 10.000 IPs com limpeza de janela sem ConcurrentModificationException")
    void deveSerThreadSafeSobAltaConcorrenciaELimpeza() throws Exception {
        RelogioMutavel relogio = new RelogioMutavel(Instant.parse("2026-11-07T12:00:00Z"));
        LimitadorPorIp limitador = new LimitadorPorIp(relogio);

        for (int i = 0; i < 10_050; i++) {
            limitador.permitir("10.0." + (i / 256) + "." + (i % 256));
        }
        relogio.avancar(Duration.ofSeconds(65));

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int id = t;
            futuros.add(pool.submit(() -> {
                largada.await(5, TimeUnit.SECONDS);
                int aceitos = 0;
                for (int i = 0; i < 100; i++) {
                    if (limitador.permitir("172.16." + id + "." + i)) {
                        aceitos++;
                    }
                }
                return aceitos;
            }));
        }

        largada.countDown();
        int totalAceitos = 0;
        for (Future<Integer> futuro : futuros) {
            totalAceitos += futuro.get(15, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(threads * 100, totalAceitos);
    }
}
