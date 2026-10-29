package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarioFeriadosTest {

    @Test
    @DisplayName("Páscoa de 2026 a 2030 bate com as datas conhecidas")
    void deveCalcularAPascoa() {
        assertThat(CalendarioFeriados.pascoa(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(CalendarioFeriados.pascoa(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
        assertThat(CalendarioFeriados.pascoa(2028)).isEqualTo(LocalDate.of(2028, 4, 16));
        assertThat(CalendarioFeriados.pascoa(2029)).isEqualTo(LocalDate.of(2029, 4, 1));
        assertThat(CalendarioFeriados.pascoa(2030)).isEqualTo(LocalDate.of(2030, 4, 21));
    }

    @Test
    @DisplayName("Feriados móveis de 2026 derivam da Páscoa")
    void deveCalcularFeriadosMoveis() {
        assertThat(CalendarioFeriados.nomeNacional(LocalDate.of(2026, 2, 17))).contains("Carnaval");
        assertThat(CalendarioFeriados.nomeNacional(LocalDate.of(2026, 4, 3))).contains("Sexta-feira Santa");
        assertThat(CalendarioFeriados.nomeNacional(LocalDate.of(2026, 6, 4))).contains("Corpus Christi");
    }

    @Test
    @DisplayName("Feriados nacionais fixos valem para qualquer estado e sem UF")
    void deveReconhecerFeriadosNacionaisFixos() {
        assertThat(CalendarioFeriados.ehFeriado(null, LocalDate.of(2026, 12, 25))).isTrue();
        assertThat(CalendarioFeriados.ehFeriado("SP", LocalDate.of(2026, 9, 7))).isTrue();
        assertThat(CalendarioFeriados.ehFeriado(null, LocalDate.of(2026, 5, 1))).isTrue();
    }

    @Test
    @DisplayName("Consciência Negra é nacional a partir de 2024")
    void deveTratarConscienciaNegraComoNacionalSoDe2024() {
        assertThat(CalendarioFeriados.ehFeriado(null, LocalDate.of(2026, 11, 20))).isTrue();
        assertThat(CalendarioFeriados.ehFeriado(null, LocalDate.of(2023, 11, 20))).isFalse();
    }

    @Test
    @DisplayName("Feriado estadual só vale para a UF dele")
    void deveAplicarFeriadoEstadualSoNaUf() {
        LocalDate nove = LocalDate.of(2026, 7, 9);
        assertThat(CalendarioFeriados.ehFeriado("SP", nove)).isTrue();
        assertThat(CalendarioFeriados.ehFeriado("sp", nove)).isTrue();
        assertThat(CalendarioFeriados.ehFeriado("RJ", nove)).isFalse();
        assertThat(CalendarioFeriados.ehFeriado(null, nove)).isFalse();
        assertThat(CalendarioFeriados.ehFeriado("RS", LocalDate.of(2026, 9, 20))).isTrue();
    }

    @Test
    @DisplayName("Dia comum não é feriado")
    void naoDeveMarcarDiaComum() {
        assertThat(CalendarioFeriados.ehFeriado("SP", LocalDate.of(2026, 3, 10))).isFalse();
    }

    @Test
    @DisplayName("UF válida aceita as 27 siglas e recusa o resto")
    void deveValidarUf() {
        assertThat(CalendarioFeriados.UFS).hasSize(27);
        assertThat(CalendarioFeriados.ufValida("SP")).isTrue();
        assertThat(CalendarioFeriados.ufValida("XX")).isFalse();
        assertThat(CalendarioFeriados.ufValida(null)).isFalse();
    }
}
