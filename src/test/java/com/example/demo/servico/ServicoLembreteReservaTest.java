package com.example.demo.servico;

import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.ModoLembrete;
import com.example.demo.model.Reserva;
import com.example.demo.model.SituacaoReserva;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
import com.example.demo.repository.ContatoRepository;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoLembreteReservaTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private AuditoriaAcessoContatoRepository auditoriaRepository;

    @Mock
    private ServicoMensagem servicoMensagem;

    @InjectMocks
    private ServicoLembreteReserva servicoLembreteReserva;

    private Empresa empresa;
    private Contato contato;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        String chaveCripto = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());
        CriptografiaCampo.configurarChaves(chaveCripto, "indice-chave-teste-32bytes-ok!!");

        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setLembreteModo(ModoLembrete.SUGERIDO);

        contato = new Contato();
        contato.setId(10L);
        contato.setEmpresa(empresa);
        contato.setNome("Carlos");
        contato.setWhatsappId("5511988887777@c.us");
        contato.setNaoContatar(false);

        reserva = new Reserva();
        reserva.setId(100L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(LocalDate.of(2026, 10, 30));
        reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);
    }

    @AfterEach
    void restaurarCripto() {
        CriptografiaCampo.configurarChaves(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "chave-teste-hmac-indice-32-bytes!");
    }

    @Test
    @DisplayName("Muda reserva para PRONTA_PARA_AVISAR quando empresa usa modo SUGERIDO")
    void deveMudarParaProntaParaAvisarEmModoSugerido() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 29, 10, 0);

        when(reservaRepository.findByDataLessThanAndSituacaoNot(any(), eq(SituacaoReserva.ENCERRADA)))
                .thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviadoEmLessThanEqual(any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.ENVIANDO), any()))
                .thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.AGUARDANDO_LEMBRETE), any()))
                .thenReturn(List.of(reserva));
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.FALHA_ENVIO), any()))
                .thenReturn(List.of());

        servicoLembreteReserva.executarCiclo(agora);

        verify(reservaRepository).save(argThat(r -> r.getSituacao() == SituacaoReserva.PRONTA_PARA_AVISAR));
    }

    @Test
    @DisplayName("Encerra reservas com data anterior a hoje")
    void deveEncerrarReservasPassadas() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 29, 10, 0);
        Reserva passada = new Reserva();
        passada.setId(101L);
        passada.setData(LocalDate.of(2026, 10, 28));
        passada.setSituacao(SituacaoReserva.CONFIRMADA_PELO_CLIENTE);

        when(reservaRepository.findByDataLessThanAndSituacaoNot(agora.toLocalDate(), SituacaoReserva.ENCERRADA))
                .thenReturn(List.of(passada));

        servicoLembreteReserva.executarCiclo(agora);

        verify(reservaRepository).save(argThat(r -> r.getId().equals(101L) && r.getSituacao() == SituacaoReserva.ENCERRADA));
    }

    @Test
    @DisplayName("Envia lembrete em modo AUTOMATICO, registra FALHA_ENVIO em erro e faz retry até 3 tentativas")
    void deveEnviarEMarcarFalhaERetentarNoModoAutomatico() {
        empresa.setLembreteModo(ModoLembrete.AUTOMATICO);
        LocalDateTime agora = LocalDateTime.of(2026, 10, 29, 11, 0);

        when(servicoMensagem.tentarEnviarMensagemWPP(eq(empresa), anyString(), anyString())).thenReturn(false);
        assertFalse(servicoLembreteReserva.enviarLembrete(reserva, agora));
        assertEquals(SituacaoReserva.FALHA_ENVIO, reserva.getSituacao());
        assertEquals(1, reserva.getTentativasEnvio());

        servicoLembreteReserva.zerarContadoresParaTestes();
        when(servicoMensagem.tentarEnviarMensagemWPP(eq(empresa), anyString(), anyString())).thenReturn(true);
        when(reservaRepository.findByDataLessThanAndSituacaoNot(any(), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviadoEmLessThanEqual(any(), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.ENVIANDO), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.AGUARDANDO_LEMBRETE), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.FALHA_ENVIO), any())).thenReturn(List.of(reserva));

        servicoLembreteReserva.executarCiclo(agora);

        assertEquals(SituacaoReserva.LEMBRETE_ENVIADO, reserva.getSituacao());
        assertEquals(2, reserva.getTentativasEnvio());
    }

    @Test
    @DisplayName("Ignora empresas inativas (ativo=false) no ciclo e no envio manual")
    void deveIgnorarEmpresaInativa() {
        empresa.setAtivo(false);
        empresa.setLembreteModo(ModoLembrete.AUTOMATICO);
        LocalDateTime agora = LocalDateTime.of(2026, 10, 29, 11, 0);

        assertFalse(servicoLembreteReserva.enviarLembrete(reserva, agora));
        verify(servicoMensagem, never()).tentarEnviarMensagemWPP(any(), anyString(), anyString());

        when(reservaRepository.findByDataLessThanAndSituacaoNot(any(), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviadoEmLessThanEqual(any(), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.ENVIANDO), any())).thenReturn(List.of());
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.AGUARDANDO_LEMBRETE), any())).thenReturn(List.of(reserva));
        when(reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(eq(SituacaoReserva.FALHA_ENVIO), any())).thenReturn(List.of(reserva));

        servicoLembreteReserva.executarCiclo(agora);

        verify(servicoMensagem, never()).tentarEnviarMensagemWPP(any(), anyString(), anyString());
        assertEquals(SituacaoReserva.AGUARDANDO_LEMBRETE, reserva.getSituacao());
    }
}
