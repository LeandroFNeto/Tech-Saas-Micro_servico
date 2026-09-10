package com.example.demo.servico;

import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.EstadoUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class GerenciadorSessaoTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant INICIO_RELOGIO = Instant.parse("2026-09-09T15:00:00Z");
    private static final String CLIENTE = "5511988887777@c.us";

    private final GerenciadorSessao gerenciadorSessao = new GerenciadorSessao();

    @Test
    @DisplayName("Remetente novo começa em INICIO")
    void deveRetornarInicioPorPadrao() {
        assertThat(gerenciadorSessao.getEstado("5511999999999@c.us")).isEqualTo(EstadoUsuario.INICIO);
        assertThat(gerenciadorSessao.getEstado(null)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("JID com 9º dígito extra compartilha o mesmo estado")
    void deveUsarAMesmaChaveParaNumeroNormalizado() {
        gerenciadorSessao.setEstado("5511988887777@c.us", EstadoUsuario.ESPERANDO_DATA);

        assertThat(gerenciadorSessao.getEstado("551188887777")).isEqualTo(EstadoUsuario.ESPERANDO_DATA);
        assertThat(gerenciadorSessao.getEstado("5511988887777@c.us")).isEqualTo(EstadoUsuario.ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Rascunho da reserva fica na mesma chave normalizada")
    void deveGuardarReservaNaMesmaChave() {
        gerenciadorSessao.getOuCriarReserva("5511988887777@c.us").setHorario(DadosReservaSessao.HORARIO_DIARIA);

        assertThat(gerenciadorSessao.getReserva("551188887777").getHorario())
                .isEqualTo(DadosReservaSessao.HORARIO_DIARIA);
    }

    @Test
    @DisplayName("Voltar para INICIO limpa o rascunho da reserva")
    void deveLimparReservaAoVoltarParaInicio() {
        gerenciadorSessao.getOuCriarReserva("5511988887777@c.us").setHorario("14:00");
        gerenciadorSessao.setEstado("5511988887777@c.us", EstadoUsuario.INICIO);

        assertThat(gerenciadorSessao.getReserva("5511988887777@c.us")).isNull();
    }

    @Test
    @DisplayName("Sessão abaixo de 30 minutos preserva o estado atual")
    void deveManterEstadoQuandoTtlNaoExpirou() {
        RelogioControlado relogio = new RelogioControlado(INICIO_RELOGIO);
        GerenciadorSessao gerenciador = new GerenciadorSessao(relogio);

        gerenciador.atualizarEstado(CLIENTE, EstadoUsuario.ESPERANDO_DATA.name());
        relogio.avancarMinutos(30);

        assertThat(gerenciador.obterEstado(CLIENTE)).isEqualTo(EstadoUsuario.ESPERANDO_DATA.name());
        assertThat(gerenciador.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Sessão acima de 30 minutos volta para INICIO")
    void deveResetarEstadoQuandoTtlExpirar() {
        RelogioControlado relogio = new RelogioControlado(INICIO_RELOGIO);
        GerenciadorSessao gerenciador = new GerenciadorSessao(relogio);

        gerenciador.getOuCriarReserva(CLIENTE).setHorario("14:00");
        gerenciador.atualizarEstado(CLIENTE, EstadoUsuario.RESERVA_ESPERANDO_NOME.name());
        relogio.avancarMinutos(31);

        assertThat(gerenciador.obterEstado(CLIENTE)).isEqualTo(EstadoUsuario.INICIO.name());
        assertThat(gerenciador.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.INICIO);
        assertThat(gerenciador.getReserva(CLIENTE)).isNull();
    }

    @Test
    @DisplayName("atualizarEstado renova o TTL da sessão")
    void deveRenovarTtlAoAtualizarEstado() {
        RelogioControlado relogio = new RelogioControlado(INICIO_RELOGIO);
        GerenciadorSessao gerenciador = new GerenciadorSessao(relogio);

        gerenciador.atualizarEstado(CLIENTE, EstadoUsuario.MENU_PRINCIPAL.name());
        relogio.avancarMinutos(20);
        gerenciador.atualizarEstado(CLIENTE, EstadoUsuario.ESPERANDO_DATA.name());
        relogio.avancarMinutos(20);

        assertThat(gerenciador.obterEstado(CLIENTE)).isEqualTo(EstadoUsuario.ESPERANDO_DATA.name());
    }

    private static final class RelogioControlado extends Clock {
        private Instant instante;

        RelogioControlado(Instant instante) {
            this.instante = instante;
        }

        void avancarMinutos(long minutos) {
            instante = instante.plus(Duration.ofMinutes(minutos));
        }

        @Override
        public ZoneId getZone() {
            return FUSO;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instante, zone);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
