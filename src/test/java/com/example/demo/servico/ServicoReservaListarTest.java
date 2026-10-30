package com.example.demo.servico;

import com.example.demo.dto.DiaReservaDTO;
import com.example.demo.model.Empresa;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicoReservaListarTest {

    @Mock private ServicoGoogleagenda agendaService;
    @Mock private ServicoFuncionamento servicoFuncionamento;
    @InjectMocks private ServicoReserva servicoReserva;

    private Calendar calendar;

    @BeforeEach
    void preparar() throws Exception {
        calendar = mock(Calendar.class, RETURNS_DEEP_STUBS);
        when(agendaService.conectarAgenda()).thenReturn(calendar);
    }

    private void agendaDevolve(Events eventos) throws Exception {
        when(calendar.events().list(anyString())
                .setMaxResults(anyInt())
                .setTimeMin(any())
                .setOrderBy(anyString())
                .setSingleEvents(any())
                .execute()).thenReturn(eventos);
    }

    private static Empresa empresaComAgenda() {
        Empresa empresa = new Empresa();
        empresa.setGoogleCalendarId("agenda@group.calendar.google.com");
        return empresa;
    }

    @Test
    @DisplayName("Agenda sem eventos devolve os 30 dias como Disponível")
    void agendaSemEventosDevolveTrintaDisponiveis() throws Exception {
        agendaDevolve(new Events());

        List<DiaReservaDTO> dias = servicoReserva.listarReservas(empresaComAgenda());

        assertThat(dias).hasSize(30);
        assertThat(dias).allMatch(dia -> "Disponível".equals(dia.status()));
    }

    @Test
    @DisplayName("Evento de dia inteiro marca só aquela data como Alugado")
    void eventoMarcaDiaAlugado() throws Exception {
        LocalDate alvo = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(2);
        Event evento = new Event().setStart(new EventDateTime().setDate(new DateTime(alvo.toString())));
        agendaDevolve(new Events().setItems(List.of(evento)));

        List<DiaReservaDTO> dias = servicoReserva.listarReservas(empresaComAgenda());

        String esperado = alvo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        assertThat(dias).hasSize(30);
        assertThat(dias).filteredOn(dia -> "Alugado".equals(dia.status()))
                .extracting(DiaReservaDTO::data).containsExactly(esperado);
    }

    @Test
    @DisplayName("Erro do Google Agenda devolve nulo")
    void erroDaAgendaDevolveNulo() throws Exception {
        when(agendaService.conectarAgenda()).thenThrow(new RuntimeException("sem credencial"));

        assertThat(servicoReserva.listarReservas(empresaComAgenda())).isNull();
    }

    @Test
    @DisplayName("Empresa sem googleCalendarId devolve nulo")
    void semCalendarIdDevolveNulo() {
        assertThat(servicoReserva.listarReservas(new Empresa())).isNull();
    }
    @Test
    @DisplayName("Por hora: dia livre fica Disponível e dia lotado/fechado fica Alugado")
    void porHoraCalculaSituacaoPorDia() throws Exception {
        Empresa empresa = empresaComAgenda();
        empresa.setLocacaoPorHora(true);

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        when(agendaService.ocupacoesDoPeriodo(anyString(), any(), any())).thenReturn(List.of());

        ServicoFuncionamento.Janela janelaAberta = new ServicoFuncionamento.Janela(true, java.time.LocalTime.of(8, 0), java.time.LocalTime.of(18, 0), 60, null);
        when(servicoFuncionamento.janelaDa(any(), any())).thenReturn(janelaAberta);
        when(servicoFuncionamento.situacaoDoDia(any(), any(), any(), any())).thenReturn(ServicoFuncionamento.Situacao.LIVRE);

        List<DiaReservaDTO> dias = servicoReserva.listarReservas(empresa);

        assertThat(dias).hasSize(30);
        assertThat(dias).allMatch(d -> "Disponível".equals(d.status()));
    }
}
