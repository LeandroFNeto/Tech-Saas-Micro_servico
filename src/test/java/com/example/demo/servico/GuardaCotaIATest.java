package com.example.demo.servico;

import org.slf4j.LoggerFactory;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
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

    @Test
    @DisplayName("Informa a situação da guarda sem consumir chamada")
    void informaSituacao() {
        GuardaCotaIA g = guarda(relogio(), 1, 10, 100);
        assertThat(g.situacao()).isEqualTo(GuardaCotaIA.Situacao.LIVRE);
        assertThat(g.situacao()).isEqualTo(GuardaCotaIA.Situacao.LIVRE);

        assertThat(g.reservar()).isTrue();
        assertThat(g.situacao()).isEqualTo(GuardaCotaIA.Situacao.TETO_CHAMADAS_MINUTO);

        g.registrarCotaEsgotada();
        assertThat(g.situacao()).isEqualTo(GuardaCotaIA.Situacao.PAUSADA_APOS_429);

        GuardaCotaIA porDia = guarda(relogio(), 5, 1, 100);
        porDia.reservar();
        assertThat(porDia.situacao()).isEqualTo(GuardaCotaIA.Situacao.TETO_CHAMADAS_DIA);

        assertThat(guarda(relogio(), 5, 5, 0).situacao()).isEqualTo(GuardaCotaIA.Situacao.TETO_GASTO_DIARIO);
    }

    @Test
    @DisplayName("Chamada sem resposta soma o custo estimado fixo")
    void chamadaSemRespostaSomaCustoFixo() {
        GuardaCotaIA g = guarda(relogio(), 5, 5, 100);
        g.registrarChamadaSemResposta();
        assertThat(g.gastoDoDiaReais()).isEqualTo(0.01);
    }

    @Test
    @DisplayName("No teto de gasto registra um aviso no log, uma vez por dia e sem dado de cliente")
    void avisaNoLogNoTetoDeGasto() {
        Logger logger = (Logger) LoggerFactory.getLogger(GuardaCotaIA.class);
        ListAppender<ILoggingEvent> captura = new ListAppender<>();
        captura.start();
        logger.addAppender(captura);
        try {
            GuardaCotaIA g = guarda(relogio(), 5, 5, 0);
            assertThat(g.reservar()).isFalse();
            assertThat(g.reservar()).isFalse();

            assertThat(captura.list).hasSize(1);
            assertThat(captura.list.get(0).getLevel()).isEqualTo(Level.WARN);
            assertThat(captura.list.get(0).getFormattedMessage()).contains("Teto diario de gasto").doesNotContainPattern("\\d{8,}");
        } finally {
            logger.detachAppender(captura);
        }
    }
}
