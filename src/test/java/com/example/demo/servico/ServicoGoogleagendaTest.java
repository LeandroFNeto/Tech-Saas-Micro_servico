package com.example.demo.servico;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicoGoogleagendaTest {

    @Test
    @DisplayName("Inicializacao da agenda lida graciosamente com presenca ou ausencia de credencial.json")
    void inicializarComCredencialConectaAgenda() {
        ServicoGoogleagenda servico = new ServicoGoogleagenda();
        assertDoesNotThrow(servico::inicializarAgenda);

        boolean temCredencial = ServicoGoogleagenda.class.getResourceAsStream("/credencial.json") != null;
        if (temCredencial) {
            assertNotNull(servico.conectarAgenda());
        } else {
            assertThrows(IllegalStateException.class, servico::conectarAgenda);
        }
    }

    @Test
    @DisplayName("datasOcupadas ignora cancelled, converte DateTime UTC para America/Sao_Paulo e expande multi-dia all-day")
    void deveTratarCanceladosFusoBrasiliaEMultiDiaEmDatasOcupadas() throws Exception {
        ServicoGoogleagenda servico = new ServicoGoogleagenda();
        Calendar calendar = mock(Calendar.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(servico, "servicoGoogleEmMemoria", calendar);

        Event cancelado = new Event()
                .setStatus("cancelled")
                .setStart(new EventDateTime().setDate(new DateTime("2026-11-10")));

        Event utcNoiteBrasilia = new Event()
                .setStatus("confirmed")
                .setStart(new EventDateTime().setDateTime(DateTime.parseRfc3339("2026-11-12T01:30:00Z")));

        Event multiDia = new Event()
                .setStatus("confirmed")
                .setStart(new EventDateTime().setDate(new DateTime("2026-11-20")))
                .setEnd(new EventDateTime().setDate(new DateTime("2026-11-23")));

        when(calendar.events().list(anyString())
                .setTimeMin(any())
                .setTimeMax(any())
                .setSingleEvents(any())
                .setMaxResults(anyInt())
                .execute()).thenReturn(new Events().setItems(List.of(cancelado, utcNoiteBrasilia, multiDia)));

        Set<LocalDate> ocupadas = servico.datasOcupadas(
                "agenda@group.calendar.google.com",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30));

        assertEquals(Set.of(
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 21),
                LocalDate.of(2026, 11, 22)
        ), ocupadas);
    }
}
