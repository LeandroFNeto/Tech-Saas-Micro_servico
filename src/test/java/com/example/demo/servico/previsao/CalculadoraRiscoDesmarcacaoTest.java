package com.example.demo.servico.previsao;

import com.example.demo.servico.previsao.CalculadoraRiscoDesmarcacao.Risco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraRiscoDesmarcacaoTest {

    @Test
    @DisplayName("Chance de 69% sem chuva relevante fica em MEDIO e 70% sobe para ALTO")
    void limiteDaChanceDeChuva() {
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.69, 0, 800)).isEqualTo(Risco.MEDIO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.70, 0, 800)).isEqualTo(Risco.ALTO);
    }

    @Test
    @DisplayName("Chance de 39% é BAIXO e 40% é MEDIO")
    void limiteInferiorDoMedio() {
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.39, 0, 800)).isEqualTo(Risco.BAIXO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.40, 0, 800)).isEqualTo(Risco.MEDIO);
    }

    @Test
    @DisplayName("Chuva de 9,9 mm é MEDIO e 10 mm é ALTO")
    void limiteDosMilimetrosAlto() {
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 9.9, 500)).isEqualTo(Risco.MEDIO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 10.0, 500)).isEqualTo(Risco.ALTO);
    }

    @Test
    @DisplayName("Chuva de 1,9 mm é BAIXO e 2 mm é MEDIO")
    void limiteDosMilimetrosMedio() {
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 1.9, 500)).isEqualTo(Risco.BAIXO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 2.0, 500)).isEqualTo(Risco.MEDIO);
    }

    @ParameterizedTest(name = "código {0} temporal = {1}")
    @CsvSource({"199,false", "200,true", "232,true", "299,true", "300,false", "800,false"})
    @DisplayName("Temporal é o código de condição entre 200 e 299")
    void temporalPorCodigo(int codigo, boolean esperado) {
        assertThat(CalculadoraRiscoDesmarcacao.isTemporal(codigo)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Temporal vira ALTO mesmo sem chance nem milímetros")
    void temporalSozinhoEhAlto() {
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0, 0, 211)).isEqualTo(Risco.ALTO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0, 0, 300)).isEqualTo(Risco.BAIXO);
    }

    @Test
    @DisplayName("Chance é arredondada a inteiro antes de comparar")
    void arredondaChance() {
        assertThat(CalculadoraRiscoDesmarcacao.arredondarChance(0.694)).isEqualTo(69);
        assertThat(CalculadoraRiscoDesmarcacao.arredondarChance(0.696)).isEqualTo(70);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.696, 0, 800)).isEqualTo(Risco.ALTO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.694, 0, 800)).isEqualTo(Risco.MEDIO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.396, 0, 800)).isEqualTo(Risco.MEDIO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.394, 0, 800)).isEqualTo(Risco.BAIXO);
    }

    @Test
    @DisplayName("Chuva é arredondada a 1 casa antes de comparar")
    void arredondaChuva() {
        assertThat(CalculadoraRiscoDesmarcacao.arredondarChuva(9.94)).isEqualTo(9.9);
        assertThat(CalculadoraRiscoDesmarcacao.arredondarChuva(9.95)).isEqualTo(10.0);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 9.95, 500)).isEqualTo(Risco.ALTO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 9.94, 500)).isEqualTo(Risco.MEDIO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 1.95, 500)).isEqualTo(Risco.MEDIO);
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0.10, 1.94, 500)).isEqualTo(Risco.BAIXO);
    }

    @Test
    @DisplayName("Dia sem chuva e céu limpo é BAIXO")
    void diaSeco() {
        assertThat(CalculadoraRiscoDesmarcacao.classificar(0, 0, 800)).isEqualTo(Risco.BAIXO);
    }
}
