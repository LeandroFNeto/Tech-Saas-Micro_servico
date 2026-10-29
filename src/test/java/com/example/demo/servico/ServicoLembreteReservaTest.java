package com.example.demo.servico;

import com.example.demo.model.*;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        contato.setNaoContatar(false);

        reserva = new Reserva();
        reserva.setId(100L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(LocalDate.now().plusDays(1));
        reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);
    }

    @Test
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
}
