package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteTentativasLoginTest {

    private static final class RelogioMutavel extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

        void avancarMinutos(long minutos) {
            instante.updateAndGet(i -> i.plusSeconds(minutos * 60));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    @Test
    @DisplayName("Bloqueia a sexta tentativa com tempo de espera e libera depois da janela")
    void bloqueiaAposCincoFalhas() {
        RelogioMutavel relogio = new RelogioMutavel();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);
        for (int i = 0; i < 5; i++) {
            limite.exigirLivre("a@b.com", "1.1.1.1");
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }

        LoginBloqueadoException bloqueio = assertThrows(LoginBloqueadoException.class,
                () -> limite.exigirLivre("A@b.com", "1.1.1.1"));
        assertTrue(bloqueio.getSegundosParaTentarDeNovo() > 0);

        relogio.avancarMinutos(16);
        assertDoesNotThrow(() -> limite.exigirLivre("a@b.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("A contagem é por e-mail e IP")
    void contagemPorEmailEIp() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel());
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }
        assertDoesNotThrow(() -> limite.exigirLivre("a@b.com", "2.2.2.2"));
        assertDoesNotThrow(() -> limite.exigirLivre("c@d.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("Login certo zera a contagem")
    void zeraContagem() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel());
        for (int i = 0; i < 4; i++) {
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }
        limite.zerar("a@b.com", "1.1.1.1");
        limite.registrarFalha("a@b.com", "1.1.1.1");
        assertDoesNotThrow(() -> limite.exigirLivre("a@b.com", "1.1.1.1"));
    }
}
