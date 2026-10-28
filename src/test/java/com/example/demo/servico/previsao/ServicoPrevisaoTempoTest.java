package com.example.demo.servico.previsao;

import com.example.demo.dto.DiaReservaDTO;
import com.example.demo.dto.previsao.PrevisaoDiaDTO;
import com.example.demo.dto.previsao.PrevisaoTempoResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.servico.ServicoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicoPrevisaoTempoTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ProvedorPrevisao provedor;
    private ServicoReserva servicoReserva;
    private RelogioMutavel relogio;
    private ServicoPrevisaoTempo servico;

    @BeforeEach
    void preparar() {
        provedor = mock(ProvedorPrevisao.class);
        servicoReserva = mock(ServicoReserva.class);
        relogio = new RelogioMutavel(Instant.parse("2026-10-27T15:00:00Z"));
        servico = new ServicoPrevisaoTempo(provedor, servicoReserva, relogio);
    }

    @Test
    @DisplayName("Segunda consulta dentro de 8 h usa o cache e não chama o provedor")
    void cacheDentroDeOitoHoras() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));
        Empresa empresa = empresa(1L, -23.11, -46.55, null);

        servico.consultar(empresa);
        relogio.avancar(Duration.ofHours(7).plusMinutes(59));
        servico.consultar(empresa);

        verify(provedor, times(1)).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Após 8 h o cache vence e o provedor é chamado de novo")
    void cacheVenceEmOitoHoras() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));
        Empresa empresa = empresa(1L, -23.11, -46.55, null);

        servico.consultar(empresa);
        relogio.avancar(Duration.ofHours(8));
        servico.consultar(empresa);

        verify(provedor, times(2)).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Empresas da mesma coordenada arredondada compartilham a entrada de cache")
    void cacheCompartilhadoEntreEmpresas() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));

        servico.consultar(empresa(1L, -23.1101, -46.5499, null));
        servico.consultar(empresa(2L, -23.1099, -46.5501, null));

        verify(provedor, times(1)).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Coordenadas de cidades diferentes abrem entradas separadas")
    void cacheSeparadoPorCoordenada() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));

        servico.consultar(empresa(1L, -23.11, -46.55, null));
        servico.consultar(empresa(2L, -22.90, -47.06, null));

        verify(provedor, times(2)).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("O cache não guarda a ocupação: cada empresa vê só as suas reservas")
    void cacheNaoGuardaOcupacao() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));
        LocalDate hoje = hoje();
        Empresa comReserva = empresa(1L, -23.11, -46.55, "agenda-1");
        Empresa semReserva = empresa(2L, -23.11, -46.55, "agenda-2");
        when(servicoReserva.listarReservas(comReserva)).thenReturn(reservas(hoje, hoje.plusDays(1)));
        when(servicoReserva.listarReservas(semReserva)).thenReturn(reservas(hoje, hoje));

        PrevisaoTempoResponseDTO primeira = servico.consultar(comReserva);
        PrevisaoTempoResponseDTO segunda = servico.consultar(semReserva);

        assertThat(primeira.dias().get(1).reservado()).isTrue();
        assertThat(segunda.dias().get(1).reservado()).isFalse();
        verify(provedor, times(1)).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Cruza os dias Alugado da agenda com a previsão por data")
    void cruzaComDiasReservados() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(4));
        LocalDate hoje = hoje();
        Empresa empresa = empresa(1L, -23.11, -46.55, "agenda");
        when(servicoReserva.listarReservas(empresa)).thenReturn(reservas(hoje, hoje.plusDays(2)));

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa);

        assertThat(resposta.disponivel()).isTrue();
        assertThat(resposta.reservasDisponiveis()).isTrue();
        assertThat(resposta.dias()).extracting(PrevisaoDiaDTO::reservado)
                .containsExactly(false, false, true, false);
    }

    @Test
    @DisplayName("Falha do provedor sem cache devolve indisponível e lista vazia")
    void falhaSemCache() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenThrow(new ProvedorIndisponivelException("fora"));

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa(1L, -23.11, -46.55, null));

        assertThat(resposta.disponivel()).isFalse();
        assertThat(resposta.dias()).isEmpty();
        assertThat(resposta.atualizadoEm()).isNull();
    }

    @Test
    @DisplayName("Falha do provedor com entrada vencida devolve a última entrada com o horário real dela")
    void falhaComCacheVencido() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble()))
                .thenReturn(diasAPartirDeHoje(3))
                .thenThrow(new ProvedorIndisponivelException("fora"));
        Empresa empresa = empresa(1L, -23.11, -46.55, null);
        LocalDateTime primeiroHorario = LocalDateTime.ofInstant(relogio.instant(), SAO_PAULO);

        servico.consultar(empresa);
        relogio.avancar(Duration.ofHours(9));
        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa);

        assertThat(resposta.disponivel()).isTrue();
        assertThat(resposta.dias()).hasSize(3);
        assertThat(resposta.atualizadoEm()).isEqualTo(primeiroHorario);
    }

    @Test
    @DisplayName("Perto da meia-noite, antes de 00:00 em São Paulo, o dia 27 ainda aparece")
    void antesDaMeiaNoiteEmSaoPaulo() {
        relogio.definir(Instant.parse("2026-10-28T02:30:00Z"));
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(List.of(
                dia(LocalDate.of(2026, 10, 26)),
                dia(LocalDate.of(2026, 10, 27)),
                dia(LocalDate.of(2026, 10, 28))));

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa(1L, -23.11, -46.55, null));

        assertThat(resposta.dias()).extracting(PrevisaoDiaDTO::data)
                .containsExactly(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28));
    }

    @Test
    @DisplayName("Perto da meia-noite, depois de 00:00 em São Paulo, o dia 27 sai da lista")
    void depoisDaMeiaNoiteEmSaoPaulo() {
        relogio.definir(Instant.parse("2026-10-28T03:30:00Z"));
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(List.of(
                dia(LocalDate.of(2026, 10, 27)),
                dia(LocalDate.of(2026, 10, 28)),
                dia(LocalDate.of(2026, 10, 29))));

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa(1L, -23.11, -46.55, null));

        assertThat(resposta.dias()).extracting(PrevisaoDiaDTO::data)
                .containsExactly(LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 29));
    }

    @Test
    @DisplayName("listarReservas nulo com agenda configurada marca reservas indisponíveis e nenhum cartão reservado")
    void listarReservasNuloComAgenda() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));
        Empresa empresa = empresa(1L, -23.11, -46.55, "agenda");
        when(servicoReserva.listarReservas(empresa)).thenReturn(null);

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa);

        assertThat(resposta.disponivel()).isTrue();
        assertThat(resposta.reservasDisponiveis()).isFalse();
        assertThat(resposta.dias()).hasSize(3).allMatch(dia -> !dia.reservado());
    }

    @Test
    @DisplayName("Empresa sem agenda não consulta reservas e mantém reservasDisponiveis")
    void empresaSemAgenda() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(3));

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa(1L, -23.11, -46.55, null));

        assertThat(resposta.reservasDisponiveis()).isTrue();
        assertThat(resposta.dias()).allMatch(dia -> !dia.reservado());
        verify(servicoReserva, never()).listarReservas(any());
    }

    @Test
    @DisplayName("Interruptor desligado ou nulo devolve indisponível sem chamar o provedor")
    void interruptorDesligado() {
        Empresa desligada = empresa(1L, -23.11, -46.55, null);
        desligada.setPrevisaoTempoAtiva(false);
        Empresa nula = empresa(2L, -23.11, -46.55, null);
        nula.setPrevisaoTempoAtiva(null);

        assertThat(servico.consultar(desligada).disponivel()).isFalse();
        assertThat(servico.consultar(nula).disponivel()).isFalse();
        verify(provedor, never()).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Sem cidade ou sem coordenada devolve indisponível")
    void semCidade() {
        Empresa semCidade = empresa(1L, -23.11, -46.55, null);
        semCidade.setCidadePrevisao(null);
        Empresa semCoordenada = empresa(2L, -23.11, -46.55, null);
        semCoordenada.setLatitude(null);

        assertThat(servico.consultar(semCidade).disponivel()).isFalse();
        assertThat(servico.consultar(semCoordenada).disponivel()).isFalse();
        verify(provedor, never()).previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Teto de 3 chamadas por cidade por dia; a quarta não chama o provedor e o dia seguinte zera")
    void tetoDeChamadasPorCidade() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenThrow(new ProvedorIndisponivelException("fora"));
        Empresa empresa = empresa(1L, -23.11, -46.55, null);

        for (int i = 0; i < 5; i++) {
            servico.consultar(empresa);
        }
        verify(provedor, times(ServicoPrevisaoTempo.TETO_CHAMADAS_POR_CIDADE_DIA))
                .previsaoDiaria(anyDouble(), anyDouble());

        relogio.avancar(Duration.ofHours(24));
        servico.consultar(empresa);
        verify(provedor, times(ServicoPrevisaoTempo.TETO_CHAMADAS_POR_CIDADE_DIA + 1))
                .previsaoDiaria(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Do 6º dia em diante o cartão é estimativa e a lista tem no máximo 8 dias")
    void estimativaEMaximoDeDias() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(diasAPartirDeHoje(10));

        PrevisaoTempoResponseDTO resposta = servico.consultar(empresa(1L, -23.11, -46.55, null));

        assertThat(resposta.dias()).hasSize(8);
        assertThat(resposta.dias()).extracting(PrevisaoDiaDTO::estimativa)
                .containsExactly(false, false, false, false, false, true, true, true);
    }

    @Test
    @DisplayName("Cada dia traz chance arredondada, chuva com 1 casa e risco calculado")
    void diaTrazRiscoCalculado() {
        when(provedor.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(List.of(
                new ProvedorPrevisao.DiaBruto(hoje(), "chuva forte", 502, 25.0, 18.0, 0.696, 9.95)));

        PrevisaoDiaDTO dia = servico.consultar(empresa(1L, -23.11, -46.55, null)).dias().get(0);

        assertThat(dia.chanceChuva()).isEqualTo(70);
        assertThat(dia.chuvaMm()).isEqualTo(10.0);
        assertThat(dia.risco()).isEqualTo("ALTO");
    }

    @Test
    @DisplayName("Troca de cidade: a sexta no mesmo dia é recusada e o dia seguinte zera o contador")
    void limiteDeTrocasDeCidade() {
        for (int i = 0; i < ServicoPrevisaoTempo.LIMITE_TROCAS_CIDADE_DIA; i++) {
            assertThat(servico.consumirTrocaDeCidade(1L)).isTrue();
        }
        assertThat(servico.consumirTrocaDeCidade(1L)).isFalse();
        assertThat(servico.consumirTrocaDeCidade(2L)).isTrue();

        relogio.avancar(Duration.ofHours(24));
        assertThat(servico.consumirTrocaDeCidade(1L)).isTrue();
    }

    private LocalDate hoje() {
        return LocalDate.ofInstant(relogio.instant(), SAO_PAULO);
    }

    private List<ProvedorPrevisao.DiaBruto> diasAPartirDeHoje(int quantidade) {
        List<ProvedorPrevisao.DiaBruto> dias = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            dias.add(dia(hoje().plusDays(i)));
        }
        return dias;
    }

    private static ProvedorPrevisao.DiaBruto dia(LocalDate data) {
        return new ProvedorPrevisao.DiaBruto(data, "céu limpo", 800, 30.0, 20.0, 0.1, 0.0);
    }

    private static List<DiaReservaDTO> reservas(LocalDate base, LocalDate... alugados) {
        List<DiaReservaDTO> lista = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate data = base.plusDays(i);
            boolean alugado = List.of(alugados).contains(data);
            lista.add(new DiaReservaDTO(data.format(FORMATO), "dia", alugado ? "Alugado" : "Disponível"));
        }
        return lista;
    }

    private static Empresa empresa(Long id, double latitude, double longitude, String calendarId) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNome("Empresa " + id);
        empresa.setCidadePrevisao("Atibaia");
        empresa.setCidadePrevisaoResolvida("Atibaia, São Paulo, BR");
        empresa.setLatitude(latitude);
        empresa.setLongitude(longitude);
        empresa.setPrevisaoTempoAtiva(true);
        empresa.setGoogleCalendarId(calendarId);
        return empresa;
    }

    private static final class RelogioMutavel extends Clock {

        private Instant agora;

        RelogioMutavel(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        void definir(Instant novo) {
            agora = novo;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return zona;
                }

                @Override
                public Clock withZone(ZoneId outra) {
                    return RelogioMutavel.this.withZone(outra);
                }

                @Override
                public Instant instant() {
                    return agora;
                }
            };
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
