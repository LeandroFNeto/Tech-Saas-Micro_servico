package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GuardaCotaIATest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static class RelogioAjustavel extends Clock {
        private Instant agora;

        RelogioAjustavel(Instant inicio) {
            this.agora = inicio;
        }

        void avancarSegundos(long s) {
            agora = agora.plusSeconds(s);
        }

        @Override
        public ZoneId getZone() {
            return FUSO;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private GuardaCotaIA guarda(RelogioAjustavel relogio, int porMinuto, int porDia, double teto) {
        return new GuardaCotaIA(porMinuto, porDia, teto, 0.30, 2.50, 5.50, 0.01, 300, relogio);
    }

    private RelogioAjustavel relogio() {
        return new RelogioAjustavel(Instant.parse("2026-11-07T15:00:00Z"));
    }

    @Test
    @DisplayName("Bloqueia ao passar do teto por minuto e libera no minuto seguinte")
    void tetoPorMinuto() {
        RelogioAjustavel r = relogio();
        GuardaCotaIA g = guarda(r, 2, 100, 100);

        assertThat(g.reservar()).isTrue();
        assertThat(g.reservar()).isTrue();
        assertThat(g.reservar()).isFalse();

        r.avancarSegundos(61);
        assertThat(g.reservar()).isTrue();
    }

    @Test
    @DisplayName("Bloqueia ao passar do teto por dia")
    void tetoPorDia() {
        RelogioAjustavel r = relogio();
        GuardaCotaIA g = guarda(r, 100, 2, 100);

        assertThat(g.reservar()).isTrue();
        assertThat(g.reservar()).isTrue();
        assertThat(g.reservar()).isFalse();
    }

    @Test
    @DisplayName("Pausa depois do 429 e volta quando a pausa termina")
    void pausaAposCotaEsgotada() {
        RelogioAjustavel r = relogio();
        GuardaCotaIA g = guarda(r, 100, 100, 100);

        g.registrarCotaEsgotada();
        assertThat(g.reservar()).isFalse();

        r.avancarSegundos(301);
        assertThat(g.reservar()).isTrue();
    }

    @Test
    @DisplayName("Bloqueia no teto de gasto em reais e zera no dia seguinte")
    void tetoDeGastoDiario() {
        RelogioAjustavel r = relogio();
        GuardaCotaIA g = guarda(r, 100, 100, 1.0);

        assertThat(g.reservar()).isTrue();
        g.registrarUso(1_000_000, 200_000);
        assertThat(g.gastoDoDiaReais()).isGreaterThan(1.0);
        assertThat(g.reservar()).isFalse();

        r.avancarSegundos(24 * 3600);
        assertThat(g.gastoDoDiaReais()).isZero();
        assertThat(g.reservar()).isTrue();
    }

    @Test
    @DisplayName("Sem tokens informados soma o custo estimado fixo")
    void custoEstimadoSemTokens() {
        GuardaCotaIA g = guarda(relogio(), 100, 100, 100);

        g.registrarUso(null, null);

        assertThat(g.gastoDoDiaReais()).isEqualTo(0.01);
    }

    @Test
    @DisplayName("Calcula o gasto pelos tokens, preços e cotação")
    void calculaGastoPelosTokens() {
        GuardaCotaIA g = guarda(relogio(), 100, 100, 100);

        g.registrarUso(1_000_000, 1_000_000);

        assertThat(g.gastoDoDiaReais()).isEqualTo((0.30 + 2.50) * 5.50);
    }
}
