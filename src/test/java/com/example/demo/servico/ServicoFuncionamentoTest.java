package com.example.demo.servico;

import com.example.demo.model.DataEspecial;
import com.example.demo.model.Empresa;
import com.example.demo.model.HorarioFuncionamento;
import com.example.demo.model.PerfilFuncionamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoFuncionamentoTest {

    private static final LocalDate QUARTA = LocalDate.of(2026, 3, 11);
    private static final LocalDate SABADO = LocalDate.of(2026, 3, 14);
    private static final LocalDate NATAL = LocalDate.of(2026, 12, 25);

    private static HorarioFuncionamento linha(PerfilFuncionamento perfil, String abre, String fecha, Integer duracao) {
        HorarioFuncionamento linha = new HorarioFuncionamento();
        linha.setPerfil(perfil);
        linha.setAbre(LocalTime.parse(abre));
        linha.setFecha(LocalTime.parse(fecha));
        linha.setDuracaoMinutos(duracao);
        return linha;
    }

    private static HorarioFuncionamento fechada(PerfilFuncionamento perfil) {
        HorarioFuncionamento linha = new HorarioFuncionamento();
        linha.setPerfil(perfil);
        linha.setFechado(true);
        return linha;
    }

    @Test
    @DisplayName("situacaoDoDia: fechado, lotado quando todo início conflita, livre quando sobra um")
    void situacaoDoDiaDistingueFechadoLotadoELivre() {
        ServicoFuncionamento servico = new ServicoFuncionamento();
        Empresa empresa = new Empresa();
        LocalDate dia = LocalDate.of(2026, 11, 10);
        ServicoFuncionamento.Janela janela = new ServicoFuncionamento.Janela(
                true, LocalTime.of(8, 0), LocalTime.of(10, 0), 60, null);
        ServicoGoogleagenda.Intervalo manha = new ServicoGoogleagenda.Intervalo(
                dia.atTime(8, 0), dia.atTime(9, 0));
        ServicoGoogleagenda.Intervalo tudo = new ServicoGoogleagenda.Intervalo(
                dia.atTime(8, 0), dia.atTime(10, 0));

        assertThat(servico.situacaoDoDia(empresa, dia, ServicoFuncionamento.Janela.fechada("x"), List.of()))
                .isEqualTo(ServicoFuncionamento.Situacao.FECHADO);
        assertThat(servico.situacaoDoDia(empresa, dia, janela, List.of(manha)))
                .isEqualTo(ServicoFuncionamento.Situacao.LIVRE);
        assertThat(servico.situacaoDoDia(empresa, dia, janela, List.of(tudo)))
                .isEqualTo(ServicoFuncionamento.Situacao.LOTADO);
    }

    @Test
    @DisplayName("Sem configuração vale 08:00 às 22:00 com a duração padrão de 60 minutos")
    void deveUsarPadroesQuandoNadaFoiConfigurado() {
        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(new Empresa(), QUARTA, null, List.of());

        assertThat(janela.aberto()).isTrue();
        assertThat(janela.abre()).isEqualTo(LocalTime.of(8, 0));
        assertThat(janela.fecha()).isEqualTo(LocalTime.of(22, 0));
        assertThat(janela.duracaoMinutos()).isEqualTo(60);
    }

    @Test
    @DisplayName("A duração da empresa vale quando o perfil não traz a própria")
    void deveHerdarADuracaoDaEmpresa() {
        Empresa empresa = new Empresa();
        empresa.setDuracaoAtendimentoMinutos(90);

        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                empresa, QUARTA, null, List.of(linha(PerfilFuncionamento.QUARTA, "09:00", "18:00", null)));

        assertThat(janela.duracaoMinutos()).isEqualTo(90);
        assertThat(janela.abre()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("O perfil do sábado vale no sábado e traz a duração própria")
    void deveUsarOPerfilDoDiaDaSemana() {
        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                new Empresa(), SABADO, null, List.of(
                        linha(PerfilFuncionamento.QUARTA, "08:00", "20:00", null),
                        linha(PerfilFuncionamento.SABADO, "10:00", "18:00", 120)));

        assertThat(janela.abre()).isEqualTo(LocalTime.of(10, 0));
        assertThat(janela.duracaoMinutos()).isEqualTo(120);
    }

    @Test
    @DisplayName("Dia da semana fechado devolve janela fechada")
    void deveFecharODiaDaSemana() {
        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                new Empresa(), QUARTA, null, List.of(fechada(PerfilFuncionamento.QUARTA)));

        assertThat(janela.aberto()).isFalse();
    }

    @Test
    @DisplayName("Feriado nacional usa o perfil FERIADO e leva o nome ao motivo")
    void deveUsarOPerfilFeriadoNoFeriadoNacional() {
        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                new Empresa(), NATAL, null, List.of(fechada(PerfilFuncionamento.FERIADO)));

        assertThat(janela.aberto()).isFalse();
        assertThat(janela.motivo()).isEqualTo("feriado: Natal");
    }

    @Test
    @DisplayName("Sem perfil FERIADO configurado o feriado segue o domingo")
    void deveSeguirODomingoQuandoNaoHaPerfilFeriado() {
        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                new Empresa(), NATAL, null, List.of(linha(PerfilFuncionamento.DOMINGO, "10:00", "14:00", null)));

        assertThat(janela.abre()).isEqualTo(LocalTime.of(10, 0));
        assertThat(janela.fecha()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    @DisplayName("Feriado estadual só vale com a UF da empresa")
    void deveAplicarFeriadoEstadualComAUf() {
        LocalDate noveDeJulho = LocalDate.of(2026, 7, 9);
        Empresa paulista = new Empresa();
        paulista.setUf("SP");
        List<HorarioFuncionamento> linhas = List.of(fechada(PerfilFuncionamento.FERIADO));

        assertThat(ServicoFuncionamento.calcular(paulista, noveDeJulho, null, linhas).aberto()).isFalse();
        assertThat(ServicoFuncionamento.calcular(new Empresa(), noveDeJulho, null, linhas).aberto()).isTrue();
    }

    @Test
    @DisplayName("Dia especial fechado vence o perfil e o feriado, com o motivo da empresa")
    void deveDarPrioridadeAoDiaEspecialFechado() {
        DataEspecial especial = new DataEspecial();
        especial.setData(NATAL);
        especial.setFechado(true);
        especial.setMotivo("Recesso");

        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                new Empresa(), NATAL, especial, List.of(linha(PerfilFuncionamento.FERIADO, "08:00", "22:00", null)));

        assertThat(janela.aberto()).isFalse();
        assertThat(janela.motivo()).isEqualTo("Recesso");
    }

    @Test
    @DisplayName("Dia especial com horário próprio vence o perfil")
    void deveDarPrioridadeAoHorarioDoDiaEspecial() {
        DataEspecial especial = new DataEspecial();
        especial.setData(QUARTA);
        especial.setAbre(LocalTime.of(12, 0));
        especial.setFecha(LocalTime.of(16, 0));

        ServicoFuncionamento.Janela janela = ServicoFuncionamento.calcular(
                new Empresa(), QUARTA, especial, List.of(linha(PerfilFuncionamento.QUARTA, "08:00", "22:00", null)));

        assertThat(janela.abre()).isEqualTo(LocalTime.of(12, 0));
        assertThat(janela.fecha()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    @DisplayName("Os horários de início seguem a duração e terminam antes do fechamento")
    void deveGerarOsInicios() {
        ServicoFuncionamento.Janela janela = new ServicoFuncionamento.Janela(
                true, LocalTime.of(8, 0), LocalTime.of(11, 0), 90, null);

        assertThat(ServicoFuncionamento.iniciosPossiveis(janela))
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Janela fechada não gera horários e a janela aberta só comporta início dentro do expediente")
    void deveChecarSeAJanelaComporta() {
        assertThat(ServicoFuncionamento.iniciosPossiveis(ServicoFuncionamento.Janela.fechada("folga"))).isEmpty();

        ServicoFuncionamento.Janela janela = new ServicoFuncionamento.Janela(
                true, LocalTime.of(8, 0), LocalTime.of(22, 0), 60, null);
        assertThat(janela.comporta(LocalTime.of(21, 0))).isTrue();
        assertThat(janela.comporta(LocalTime.of(21, 30))).isFalse();
        assertThat(janela.comporta(LocalTime.of(7, 0))).isFalse();
    }
}
