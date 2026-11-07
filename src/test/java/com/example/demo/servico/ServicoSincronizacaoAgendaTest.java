package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.ModoLembrete;
import com.example.demo.model.Reserva;
import com.example.demo.model.SituacaoReserva;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoSincronizacaoAgendaTest {

    @Mock
    private ServicoReserva servicoReserva;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private ServicoSincronizacaoAgenda servicoSincronizacaoAgenda;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        String chaveCripto = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());
        CriptografiaCampo.configurarChaves(chaveCripto, "indice-chave-teste-32bytes-ok!!");

        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setGoogleCalendarId("calendar123@group.calendar.google.com");
        empresa.setLembreteModo(ModoLembrete.AUTOMATICO);
    }

    @AfterEach
    void restaurarCripto() {
        CriptografiaCampo.configurarChaves(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "chave-teste-hmac-indice-32-bytes!");
    }

    @Test
    @DisplayName("Ignora sincronização quando empresa não possui googleCalendarId")
    void deveIgnorarSincronizacaoQuandoEmpresaSemCalendarId() {
        empresa.setGoogleCalendarId(null);
        boolean resultado = servicoSincronizacaoAgenda.sincronizarAgenda(empresa, 7, false);
        assertTrue(resultado);
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Processa eventos ativos da agenda e grava reservas")
    void deveProcessarEventosAtivosESalvarReservas() throws Exception {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        ServicoReserva.EventoAgendaDTO evento = new ServicoReserva.EventoAgendaDTO(
                "evento-1",
                "João Silva - (11) 98765-4321",
                "Reserva confirmada",
                hoje.plusDays(2),
                "14:00 - 15:00",
                "confirmed"
        );

        when(servicoReserva.listarEventosProximos(empresa, 7)).thenReturn(List.of(evento));
        when(reservaRepository.findByEmpresaIdAndGoogleEventId(1L, "evento-1")).thenReturn(Optional.empty());

        boolean resultado = servicoSincronizacaoAgenda.sincronizarAgenda(empresa, 7, true);

        assertTrue(resultado);
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Cancela reserva local quando evento vem cancelled ou desaparece da agenda")
    void deveMarcarCanceladaNaAgendaQuandoEventoCanceladoOuAusente() throws Exception {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Reserva canceladaExplicita = new Reserva();
        canceladaExplicita.setGoogleEventId("ev-cancelado");
        canceladaExplicita.setData(hoje.plusDays(3));
        canceladaExplicita.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);

        Reserva ausenteNaAgenda = new Reserva();
        ausenteNaAgenda.setGoogleEventId("ev-sumiu");
        ausenteNaAgenda.setData(hoje.plusDays(4));
        ausenteNaAgenda.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);

        ServicoReserva.EventoAgendaDTO evCancelado = new ServicoReserva.EventoAgendaDTO(
                "ev-cancelado", "Cancelado", "", hoje.plusDays(3), "10:00", "cancelled");

        when(servicoReserva.listarEventosProximos(empresa, 7)).thenReturn(List.of(evCancelado));
        when(reservaRepository.findByEmpresaIdAndGoogleEventId(1L, "ev-cancelado")).thenReturn(Optional.of(canceladaExplicita));
        when(reservaRepository.findByEmpresaIdAndDataBetweenOrderByDataAscHorarioAsc(anyLong(), any(), any()))
                .thenReturn(List.of(ausenteNaAgenda));

        assertTrue(servicoSincronizacaoAgenda.sincronizarAgenda(empresa, 7, true));
        assertEquals(SituacaoReserva.CANCELADA_NA_AGENDA, canceladaExplicita.getSituacao());
        assertEquals(SituacaoReserva.CANCELADA_NA_AGENDA, ausenteNaAgenda.getSituacao());
    }

    @Test
    @DisplayName("Ignora empresas inativas (ativo=false) em todas as rotinas de sincronização")
    void deveIgnorarEmpresaInativa() throws Exception {
        empresa.setAtivo(false);

        assertFalse(servicoSincronizacaoAgenda.sincronizarSeNecessario(empresa, 7));
        assertFalse(servicoSincronizacaoAgenda.sincronizarAgenda(empresa, 7, true));

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        servicoSincronizacaoAgenda.sincronizarDeHoraEmHora();

        verify(servicoReserva, never()).listarEventosProximos(any(), anyInt());
    }
}
