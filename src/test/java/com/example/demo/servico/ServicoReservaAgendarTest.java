package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicoReservaAgendarTest {

    private static final String AGENDA = "agenda@group.calendar.google.com";

    @Mock private ServicoGoogleagenda agendaService;
    @InjectMocks private ServicoReserva servicoReserva;

    private Calendar calendar;

    @BeforeEach
    void preparar() throws Exception {
        calendar = mock(Calendar.class, RETURNS_DEEP_STUBS);
        when(agendaService.conectarAgenda()).thenReturn(calendar);
    }

    private static Empresa empresaComAgenda(String agenda) {
        Empresa empresa = new Empresa();
        empresa.setId(7L);
        empresa.setGoogleCalendarId(agenda);
        return empresa;
    }

    private Event inserir(String data, String idPedido) throws Exception {
        Event salvo = new Event().setId("salvo");
        when(calendar.events().insert(anyString(), any(Event.class)).execute()).thenReturn(salvo);
        String id = servicoReserva.agendarNoGoogle(empresaComAgenda(AGENDA), data, "Maria", "5511999990000", idPedido);
        assertThat(id).isEqualTo("salvo");
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events()).insert(eq(AGENDA), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("agendarNoGoogle monta título e descrição com a origem bot e o WhatsApp do cliente")
    void deveMontarTituloEDescricao() throws Exception {
        Event evento = inserir("15-03-2027", "pedido-1");

        assertThat(evento.getSummary()).isEqualTo("Reserva - Maria");
        assertThat(evento.getDescription()).contains("bot").contains("5511999990000");
    }

    @Test
    @DisplayName("agendarNoGoogle cria evento de dia inteiro com end igual a data mais um dia")
    void deveCriarEventoDeDiaInteiro() throws Exception {
        Event evento = inserir("28-02-2027", "pedido-2");

        assertThat(evento.getStart().getDate().toStringRfc3339()).isEqualTo("2027-02-28");
        assertThat(evento.getEnd().getDate().toStringRfc3339()).isEqualTo("2027-03-01");
        assertThat(evento.getStart().getDateTime()).isNull();
        assertThat(evento.getEnd().getDateTime()).isNull();
    }

    @Test
    @DisplayName("agendarPorHora com término à meia-noite (24:00) salva data final no dia seguinte")
    void deveAgendarPorHoraComTerminoNaMeiaNoite() throws Exception {
        Event salvo = new Event().setId("salvo-hora-meianoite");
        when(calendar.events().insert(anyString(), any(Event.class)).execute()).thenReturn(salvo);

        LocalDate dia = LocalDate.of(2027, 3, 15);
        String id = servicoReserva.agendarPorHora(
                empresaComAgenda(AGENDA), dia, LocalTime.of(22, 0), LocalTime.MIDNIGHT,
                "Carlos", "5511988887777", "pedido-h1", 0);

        assertThat(id).isEqualTo("salvo-hora-meianoite");
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events()).insert(eq(AGENDA), captor.capture());
        Event gerado = captor.getValue();
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        long esperadoInicio = dia.atTime(22, 0).atZone(fuso).toInstant().toEpochMilli();
        long esperadoFim = dia.plusDays(1).atStartOfDay().atZone(fuso).toInstant().toEpochMilli();
        assertThat(gerado.getStart().getDateTime().getValue()).isEqualTo(esperadoInicio);
        assertThat(gerado.getEnd().getDateTime().getValue()).isEqualTo(esperadoFim);
    }

    @Test
    @DisplayName("agendarPorHora diurno salva início e término no mesmo dia")
    void deveAgendarPorHoraDiurnoNoMesmoDia() throws Exception {
        Event salvo = new Event().setId("salvo-hora-dia");
        when(calendar.events().insert(anyString(), any(Event.class)).execute()).thenReturn(salvo);

        LocalDate dia = LocalDate.of(2027, 3, 15);
        String id = servicoReserva.agendarPorHora(
                empresaComAgenda(AGENDA), dia, LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Carlos", "5511988887777", "pedido-h2", 1);

        assertThat(id).isEqualTo("salvo-hora-dia");
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events()).insert(eq(AGENDA), captor.capture());
        Event gerado = captor.getValue();
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        long esperadoInicio = dia.atTime(10, 0).atZone(fuso).toInstant().toEpochMilli();
        long esperadoFim = dia.atTime(12, 0).atZone(fuso).toInstant().toEpochMilli();
        assertThat(gerado.getStart().getDateTime().getValue()).isEqualTo(esperadoInicio);
        assertThat(gerado.getEnd().getDateTime().getValue()).isEqualTo(esperadoFim);
    }

    @Test
    @DisplayName("agendarNoGoogle usa id derivado do idPedido só com base32hex minúsculo e tamanho aceito")
    void deveUsarIdBase32hexDoPedido() throws Exception {
        Event evento = inserir("15-03-2027", "pedido-3");

        assertThat(evento.getId()).matches("[0-9a-v]{5,1024}");
        assertThat(evento.getId()).isEqualTo(ServicoReserva.idEventoDoPedido("pedido-3"));
        assertThat(ServicoReserva.idEventoDoPedido("pedido-4")).isNotEqualTo(evento.getId());
    }

    @Test
    @DisplayName("agendarNoGoogle com agenda vazia ou nula lança AgendaNaoConfiguradaException sem chamar o Google")
    void deveLancarSemAgenda() throws Exception {
        for (String agenda : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> servicoReserva.agendarNoGoogle(empresaComAgenda(agenda), "15-03-2027", "Maria", "55", "p"))
                    .isInstanceOf(AgendaNaoConfiguradaException.class);
        }
        verifyNoInteractions(agendaService);
    }

    @Test
    @DisplayName("eventoDoPedidoExiste devolve true para evento existente")
    void deveDevolverTrueParaExistente() throws Exception {
        when(calendar.events().get(anyString(), anyString()).execute()).thenReturn(new Event().setStatus("confirmed"));

        assertThat(servicoReserva.eventoDoPedidoExiste(empresaComAgenda(AGENDA), "pedido-5")).isTrue();
        verify(calendar.events()).get(AGENDA, ServicoReserva.idEventoDoPedido("pedido-5"));
    }

    @Test
    @DisplayName("eventoDoPedidoExiste devolve false para evento cancelled")
    void deveDevolverFalseParaCancelado() throws Exception {
        when(calendar.events().get(anyString(), anyString()).execute()).thenReturn(new Event().setStatus("cancelled"));

        assertThat(servicoReserva.eventoDoPedidoExiste(empresaComAgenda(AGENDA), "pedido-6")).isFalse();
    }

    @Test
    @DisplayName("eventoDoPedidoExiste devolve false para 404")
    void deveDevolverFalsePara404() throws Exception {
        GoogleJsonResponseException naoEncontrado = mock(GoogleJsonResponseException.class);
        when(naoEncontrado.getStatusCode()).thenReturn(404);
        when(calendar.events().get(anyString(), anyString()).execute()).thenThrow(naoEncontrado);

        assertThat(servicoReserva.eventoDoPedidoExiste(empresaComAgenda(AGENDA), "pedido-7")).isFalse();
    }

    @Test
    @DisplayName("eventoDoPedidoExiste repassa erro diferente de 404")
    void devePropagarErroDiferenteDe404() throws Exception {
        GoogleJsonResponseException erro = mock(GoogleJsonResponseException.class);
        when(erro.getStatusCode()).thenReturn(500);
        when(calendar.events().get(anyString(), anyString()).execute()).thenThrow(erro);

        assertThatThrownBy(() -> servicoReserva.eventoDoPedidoExiste(empresaComAgenda(AGENDA), "pedido-8"))
                .isSameAs(erro);
    }

    @Test
    @DisplayName("eventoDoPedidoExiste com agenda vazia ou nula lança AgendaNaoConfiguradaException")
    void deveLancarSemAgendaAoConsultar() {
        for (String agenda : new String[]{null, ""}) {
            assertThatThrownBy(() -> servicoReserva.eventoDoPedidoExiste(empresaComAgenda(agenda), "p"))
                    .isInstanceOf(AgendaNaoConfiguradaException.class);
        }
    }
}
