package com.example.demo.servico;

import com.example.demo.servico.ServicoGoogleagenda.Intervalo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoGoogleagendaSobreposicaoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);

    private static LocalDateTime hora(int h, int m) {
        return DIA.atTime(h, m);
    }

    private static final List<Intervalo> OCUPADO_DAS_10_AS_12 = List.of(new Intervalo(hora(10, 0), hora(12, 0)));

    @Test
    @DisplayName("Horário que cruza o evento conflita")
    void deveConflitarQuandoCruza() {
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(11, 0), hora(12, 0), 0)).isTrue();
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(9, 30), hora(10, 30), 0)).isTrue();
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(10, 30), hora(11, 0), 0)).isTrue();
    }

    @Test
    @DisplayName("Terminar quando o outro começa, ou começar quando o outro termina, não conflita")
    void naoDeveConflitarNaBorda() {
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(9, 0), hora(10, 0), 0)).isFalse();
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(12, 0), hora(13, 0), 0)).isFalse();
    }

    @Test
    @DisplayName("O intervalo entre atendimentos afasta a borda nos dois lados")
    void deveSomarOIntervaloNosDoisLados() {
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(12, 0), hora(13, 0), 15)).isTrue();
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(9, 0), hora(10, 0), 15)).isTrue();
        assertThat(ServicoGoogleagenda.conflita(OCUPADO_DAS_10_AS_12, hora(12, 15), hora(13, 15), 15)).isFalse();
    }

    @Test
    @DisplayName("Evento de dia inteiro conflita com qualquer horário do dia")
    void deveConflitarComEventoDeDiaInteiro() {
        List<Intervalo> diaInteiro = List.of(new Intervalo(DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay()));

        assertThat(ServicoGoogleagenda.conflita(diaInteiro, hora(8, 0), hora(9, 0), 0)).isTrue();
        assertThat(ServicoGoogleagenda.conflita(diaInteiro, hora(21, 0), hora(22, 0), 0)).isTrue();
    }

    @Test
    @DisplayName("Agenda vazia nunca conflita")
    void naoDeveConflitarSemEventos() {
        assertThat(ServicoGoogleagenda.conflita(List.of(), hora(10, 0), hora(11, 0), 30)).isFalse();
    }

    @Test
    @DisplayName("Horário terminando à meia-noite (24:00) conflita com evento na faixa das 22h às 24h")
    void deveConflitarComTerminoNaMeiaNoite() {
        List<Intervalo> ocupadoNoite = List.of(new Intervalo(hora(22, 30), hora(23, 30)));
        assertThat(ServicoGoogleagenda.conflita(ocupadoNoite, hora(22, 0), DIA.plusDays(1).atStartOfDay(), 0)).isTrue();
    }
}
