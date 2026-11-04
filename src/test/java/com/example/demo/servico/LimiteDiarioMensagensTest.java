package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteDiarioMensagensTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final class RelogioMutavel extends Clock {
        private Instant agora = Instant.parse("2027-03-15T15:00:00Z");

        void avancarUmDia() {
            agora = agora.plusSeconds(24 * 3600);
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

    @Test
    @DisplayName("Permite até o limite do dia e bloqueia a mensagem seguinte")
    void deveBloquearAcimaDoLimite() {
        LimiteDiarioMensagens limite = new LimiteDiarioMensagens(2, new RelogioMutavel());

        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isTrue();
        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isTrue();
        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isFalse();
    }

    @Test
    @DisplayName("O limite é por número e o nono dígito não cria uma segunda cota")
    void deveContarPorNumeroNormalizado() {
        LimiteDiarioMensagens limite = new LimiteDiarioMensagens(1, new RelogioMutavel());

        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isTrue();
        assertThat(limite.registrarSePermitido("551188887777@c.us")).isFalse();
        assertThat(limite.registrarSePermitido("5511977776666@c.us")).isTrue();
    }

    @Test
    @DisplayName("No dia seguinte a cota volta")
    void deveZerarNoDiaSeguinte() {
        RelogioMutavel relogio = new RelogioMutavel();
        LimiteDiarioMensagens limite = new LimiteDiarioMensagens(1, relogio);

        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isTrue();
        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isFalse();

        relogio.avancarUmDia();

        assertThat(limite.registrarSePermitido("5511988887777@c.us")).isTrue();
    }

    @Test
    @DisplayName("Número vazio não consome cota e é recusado")
    void deveRecusarNumeroVazio() {
        LimiteDiarioMensagens limite = new LimiteDiarioMensagens(5, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThat(limite.registrarSePermitido("")).isFalse();
        assertThat(limite.registrarSePermitido(null)).isFalse();
    }
}
