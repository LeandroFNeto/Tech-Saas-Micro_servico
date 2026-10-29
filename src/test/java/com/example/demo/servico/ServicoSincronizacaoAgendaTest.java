package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.ModoLembrete;
import com.example.demo.model.Reserva;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoSincronizacaoAgendaTest {

    @Mock
    private ServicoReserva servicoReserva;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ContatoRepository contatoRepository;

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

    @Test
    void deveIgnorarSincronizacaoQuandoEmpresaSemCalendarId() {
        empresa.setGoogleCalendarId(null);
        boolean resultado = servicoSincronizacaoAgenda.sincronizarAgenda(empresa, 7, false);
        assertTrue(resultado);
        verifyNoInteractions(servicoReserva);
    }

    @Test
    void deveProcessarEventosAtivosESalvarReservas() throws Exception {
        ServicoReserva.EventoAgendaDTO evento = new ServicoReserva.EventoAgendaDTO(
                "evento-1",
                "João Silva - (11) 98765-4321",
                "Reserva confirmada",
                LocalDate.now().plusDays(2),
                "14:00 - 15:00",
                "confirmed"
        );

        when(servicoReserva.listarEventosProximos(empresa, 7)).thenReturn(List.of(evento));
        when(reservaRepository.findByEmpresaIdAndGoogleEventId(1L, "evento-1")).thenReturn(java.util.Optional.empty());

        boolean resultado = servicoSincronizacaoAgenda.sincronizarAgenda(empresa, 7, true);

        assertTrue(resultado);
        verify(reservaRepository).save(any(Reserva.class));
    }
}
