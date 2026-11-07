package com.example.demo.servico;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.model.Empresa;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.Events;
import org.junit.jupiter.api.AfterEach;
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
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicoReservaReuniaoTest {

    private static final String AGENDA = "gamb@group.calendar.google.com";
    private static final String ANA = "ana@gamb.com.br";
    private static final String LEO = "leo@gamb.com.br";
    private static final String NOVO = "novo.secreto@gamb.com.br";

    @Mock private ServicoGoogleagenda agendaService;
    @InjectMocks private ServicoReserva servicoReserva;

    private Calendar calendar;
    private Empresa empresa;
    private Logger logger;
    private ListAppender<ILoggingEvent> captura;

    @BeforeEach
    void preparar() throws Exception {
        calendar = mock(Calendar.class, RETURNS_DEEP_STUBS);
        when(agendaService.conectarAgenda()).thenReturn(calendar);
        empresa = new Empresa();
        empresa.setId(9L);
        empresa.setGoogleCalendarId(AGENDA);

        logger = (Logger) LoggerFactory.getLogger(ServicoReserva.class);
        captura = new ListAppender<>();
        captura.start();
        logger.addAppender(captura);
    }

    @AfterEach
    void encerrarCaptura() {
        logger.detachAppender(captura);
        captura.stop();
    }

    private Event reuniao(String id, String status, String... emails) {
        Event evento = new Event().setId(id).setStatus(status).setSummary("Reunião Gamb - Ajuste")
                .setStart(new com.google.api.services.calendar.model.EventDateTime()
                        .setDateTime(new DateTime(System.currentTimeMillis() + 86_400_000L)));
        List<EventAttendee> convidados = new java.util.ArrayList<>();
        for (String email : emails) {
            convidados.add(new EventAttendee().setEmail(email).setResponseStatus("accepted"));
        }
        return evento.setAttendees(convidados);
    }

    private void listaDeReunioes(Event... eventos) throws Exception {
        Events resposta = new Events().setItems(List.of(eventos));
        when(calendar.events().list(anyString()).setTimeMin(any(DateTime.class)).setSingleEvents(anyBoolean())
                .setPrivateExtendedProperty(anyList()).setMaxResults(anyInt()).execute()).thenReturn(resposta);
    }

    @Test
    @DisplayName("agendarReuniao cria o evento com os e-mails como convidados, marca de reunião e convite por e-mail")
    void deveCriarEventoComConvidados() throws Exception {
        when(calendar.events().insert(anyString(), any(Event.class)).setSendUpdates(eq("all")).execute())
                .thenReturn(new Event().setId("criado"));
        LocalDate dia = LocalDate.of(2027, 3, 15);

        String id = servicoReserva.agendarReuniao(empresa, dia, LocalTime.of(10, 0), 60, "Reunião Gamb - Ajuste",
                "Reunião marcada pelo atendente da Gamb. WhatsApp: ***7777", List.of(ANA, LEO), "reuniao-1");

        assertThat(id).isEqualTo("criado");
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events()).insert(eq(AGENDA), captor.capture());
        Event evento = captor.getValue();
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        assertThat(evento.getSummary()).isEqualTo("Reunião Gamb - Ajuste");
        assertThat(evento.getAttendees()).extracting(EventAttendee::getEmail).containsExactly(ANA, LEO);
        assertThat(evento.getExtendedProperties().getPrivate()).containsEntry("reuniaoGamb", "true");
        assertThat(evento.getStart().getDateTime().getValue())
                .isEqualTo(dia.atTime(10, 0).atZone(fuso).toInstant().toEpochMilli());
        assertThat(evento.getEnd().getDateTime().getValue())
                .isEqualTo(dia.atTime(11, 0).atZone(fuso).toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("agendarReuniao sem agenda cadastrada lança AgendaNaoConfiguradaException e não insere")
    void deveRecusarSemAgenda() throws Exception {
        empresa.setGoogleCalendarId(" ");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicoReserva.agendarReuniao(
                        empresa, LocalDate.of(2027, 3, 15), LocalTime.of(10, 0), 60, "t", "d", List.of(ANA), "r"))
                .isInstanceOf(AgendaNaoConfiguradaException.class);
        verify(calendar.events(), never()).insert(anyString(), any(Event.class));
    }

    @Test
    @DisplayName("Mudar a lista troca só os convidados: entram os novos, saem os removidos e quem fica mantém a resposta")
    void deveAtualizarSoOsConvidadosDasReunioesFuturas() throws Exception {
        listaDeReunioes(reuniao("ev1", "confirmed", ANA, LEO), reuniao("ev2", "confirmed", LEO));

        int atualizadas = servicoReserva.atualizarConvidadosDeReunioesFuturas(empresa, List.of(ANA, NOVO));

        assertThat(atualizadas).isEqualTo(2);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events(), times(2)).patch(eq(AGENDA), anyString(), captor.capture());
        Event primeiro = captor.getAllValues().get(0);
        assertThat(primeiro.getAttendees()).extracting(EventAttendee::getEmail).containsExactly(ANA, NOVO);
        assertThat(primeiro.getAttendees().get(0).getResponseStatus()).isEqualTo("accepted");
        assertThat(primeiro.getAttendees().get(1).getResponseStatus()).isNull();
        Event segundo = captor.getAllValues().get(1);
        assertThat(segundo.getAttendees()).extracting(EventAttendee::getEmail).containsExactly(ANA, NOVO);
    }

    @Test
    @DisplayName("O patch leva só os convidados: título, data e horário ficam como estão e nada é cancelado")
    void naoMexeNoEventoNemCancela() throws Exception {
        listaDeReunioes(reuniao("ev1", "confirmed", ANA));

        servicoReserva.atualizarConvidadosDeReunioesFuturas(empresa, List.of(ANA, LEO));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events()).patch(eq(AGENDA), eq("ev1"), captor.capture());
        Event corpo = captor.getValue();
        assertThat(corpo.getSummary()).isNull();
        assertThat(corpo.getStart()).isNull();
        assertThat(corpo.getEnd()).isNull();
        assertThat(corpo.getStatus()).isNull();
        verify(calendar.events(), never()).delete(anyString(), anyString());
        verify(calendar.events(), never()).insert(anyString(), any(Event.class));
        verify(calendar.events(), never()).update(anyString(), anyString(), any(Event.class));
    }

    @Test
    @DisplayName("Reunião cancelada na agenda é ignorada na atualização")
    void ignoraReuniaoCancelada() throws Exception {
        listaDeReunioes(reuniao("ev1", "cancelled", ANA));

        int atualizadas = servicoReserva.atualizarConvidadosDeReunioesFuturas(empresa, List.of(LEO));

        assertThat(atualizadas).isZero();
        verify(calendar.events(), never()).patch(anyString(), anyString(), any(Event.class));
    }

    @Test
    @DisplayName("Lista vazia remove todos os convidados das reuniões futuras sem apagar o evento")
    void listaVaziaRemoveConvidados() throws Exception {
        listaDeReunioes(reuniao("ev1", "confirmed", ANA, LEO));

        servicoReserva.atualizarConvidadosDeReunioesFuturas(empresa, List.of());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(calendar.events()).patch(eq(AGENDA), eq("ev1"), captor.capture());
        assertThat(captor.getValue().getAttendees()).isEmpty();
        verify(calendar.events(), never()).delete(anyString(), anyString());
    }

    @Test
    @DisplayName("Sem agenda cadastrada a atualização não consulta a Google Agenda")
    void semAgendaNaoConsulta() throws Exception {
        empresa.setGoogleCalendarId(null);

        assertThat(servicoReserva.atualizarConvidadosDeReunioesFuturas(empresa, List.of(ANA))).isZero();
        verify(agendaService, never()).conectarAgenda();
    }

    @Test
    @DisplayName("Nenhum e-mail de responsável aparece em log ao criar ou atualizar reuniões")
    void emailNaoApareceEmLog() throws Exception {
        when(calendar.events().insert(anyString(), any(Event.class)).setSendUpdates(eq("all")).execute())
                .thenReturn(new Event().setId("criado"));
        listaDeReunioes(reuniao("ev1", "confirmed", ANA));

        servicoReserva.agendarReuniao(empresa, LocalDate.of(2027, 3, 15), LocalTime.of(10, 0), 60, "t", "d",
                List.of(NOVO), "reuniao-2");
        servicoReserva.atualizarConvidadosDeReunioesFuturas(empresa, List.of(NOVO));

        assertThat(captura.list).isNotEmpty();
        assertThat(captura.list).noneMatch(evento -> evento.getFormattedMessage().contains("secreto")
                || evento.getFormattedMessage().contains("@gamb.com.br"));
    }
}
