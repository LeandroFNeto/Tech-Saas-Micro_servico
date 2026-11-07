package com.example.demo.servico;

import com.example.demo.servico.InterpretadorDeData.Certa;
import com.example.demo.servico.InterpretadorDeData.Duvidosa;
import com.example.demo.servico.InterpretadorDeData.Nenhuma;
import com.example.demo.servico.InterpretadorDeData.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretadorDeDataTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 4);
    private final InterpretadorDeData interpretador = new InterpretadorDeData();

    @ParameterizedTest
    @ValueSource(strings = {
            "25/12/2026", "25-12-2026", "25.12.2026", "25 12 2026", "25/12/26", "25-12-26",
            "2026-12-25", "25 de dezembro", "25 de dezembro de 2026", "25 dez", "25/12", "25-12",
            "dia 25 12", "queria o dia 25/12 pra uma festa", "Dia 25 de Dezembro, por favor"})
    @DisplayName("Reconhece a mesma data escrita de vários jeitos")
    void reconheceVariosFormatos(String texto) {
        assertThat(interpretador.interpretar(texto, HOJE)).isEqualTo(new Certa(LocalDate.of(2026, 12, 25)));
    }

    @Test
    @DisplayName("Palavras relativas")
    void palavrasRelativas() {
        assertThat(interpretador.interpretar("hoje", HOJE)).isEqualTo(new Certa(HOJE));
        assertThat(interpretador.interpretar("amanhã", HOJE)).isEqualTo(new Certa(HOJE.plusDays(1)));
        assertThat(interpretador.interpretar("depois de amanhã", HOJE)).isEqualTo(new Certa(HOJE.plusDays(2)));
    }

    @Test
    @DisplayName("Dia sem mês fica em dúvida e sugere a próxima ocorrência")
    void soODiaFicaEmDuvida() {
        assertThat(interpretador.interpretar("dia 25", HOJE)).isEqualTo(new Duvidosa(LocalDate.of(2026, 11, 25)));
        assertThat(interpretador.interpretar("dia 2", HOJE)).isEqualTo(new Duvidosa(LocalDate.of(2026, 12, 2)));
    }

    @Test
    @DisplayName("Dia da semana fica em dúvida e sugere o próximo")
    void diaDaSemanaFicaEmDuvida() {
        assertThat(interpretador.interpretar("tem sábado livre?", HOJE)).isEqualTo(new Duvidosa(LocalDate.of(2026, 11, 7)));
        assertThat(interpretador.interpretar("quarta-feira", HOJE)).isEqualTo(new Duvidosa(LocalDate.of(2026, 11, 11)));
    }

    @Test
    @DisplayName("Data sem ano que já passou neste ano fica em dúvida para o ano seguinte")
    void semAnoQueJaPassou() {
        assertThat(interpretador.interpretar("10/03", HOJE)).isEqualTo(new Duvidosa(LocalDate.of(2027, 3, 10)));
    }

    @Test
    @DisplayName("Data sem ano que ainda vem neste ano é certa")
    void semAnoQueAindaVem() {
        assertThat(interpretador.interpretar("20/11", HOJE)).isEqualTo(new Certa(LocalDate.of(2026, 11, 20)));
    }

    @Test
    @DisplayName("Data impossível fica em dúvida sem sugestão")
    void dataImpossivel() {
        assertThat(interpretador.interpretar("31/02/2027", HOJE)).isEqualTo(new Duvidosa(null));
        assertThat(interpretador.interpretar("32/01/2027", HOJE)).isEqualTo(new Duvidosa(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"oi", "bom dia", "1", "25", "quanto custa?", "somos 20 pessoas", "às 18:30", "sim", ""})
    @DisplayName("Texto sem data não é tratado como data")
    void semData(String texto) {
        Resultado resultado = interpretador.interpretar(texto, HOJE);

        assertThat(resultado).isInstanceOf(Nenhuma.class);
    }

    @Test
    @DisplayName("Texto nulo não é data")
    void nulo() {
        assertThat(interpretador.interpretar(null, HOJE)).isInstanceOf(Nenhuma.class);
    }
}
