package com.example.demo.servico;

import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.model.Reserva;
import com.example.demo.model.SituacaoReserva;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoRespostaLembreteTest {

    @Mock
    private InterpretadorRespostaLembrete interpretador;

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ServicoMensagem servicoMensagem;

    @InjectMocks
    private ServicoRespostaLembrete servicoRespostaLembrete;

    private Empresa empresa;
    private Contato contato;
    private Reserva reserva;
    private final String numeroWhatsapp = "5511999998888";

    @BeforeEach
    void setUp() {
        String chaveCripto = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());
        CriptografiaCampo.configurarChaves(chaveCripto, "indice-chave-teste-32bytes-ok!!");

        empresa = new Empresa();
        empresa.setId(10L);

        contato = new Contato();
        contato.setId(100L);
        contato.setEmpresa(empresa);

        reserva = new Reserva();
        reserva.setId(200L);
        reserva.setContato(contato);
        reserva.setData(LocalDate.now().plusDays(1));
        reserva.setSituacao(SituacaoReserva.LEMBRETE_ENVIADO);
        reserva.setLembreteEnviadoEm(LocalDateTime.now().minusHours(2));
    }

    @Test
    void deveIgnorarSeEstadoNaoForInicio() {
        boolean processou = servicoRespostaLembrete.processarPossivelResposta(
                empresa, numeroWhatsapp, "Sim", EstadoUsuario.RESERVA_ESPERANDO_NOME
        );
        assertFalse(processou);
        verifyNoInteractions(contatoRepository, reservaRepository, servicoMensagem);
    }

    @Test
    void deveIgnorarSeContatoNaoEncontrado() {
        String chave = WhatsappUtil.normalizarParaChaveEstado(numeroWhatsapp);
        String indice = CriptografiaCampo.calcularIndice(chave);

        when(contatoRepository.findByEmpresaIdAndNumeroIndice(10L, indice))
                .thenReturn(Optional.empty());

        boolean processou = servicoRespostaLembrete.processarPossivelResposta(
                empresa, numeroWhatsapp, "Sim", EstadoUsuario.INICIO
        );
        assertFalse(processou);
    }

    @Test
    void deveConfirmarReservaQuandoIntencaoForConfirma() {
        String chave = WhatsappUtil.normalizarParaChaveEstado(numeroWhatsapp);
        String indice = CriptografiaCampo.calcularIndice(chave);

        when(contatoRepository.findByEmpresaIdAndNumeroIndice(10L, indice))
                .thenReturn(Optional.of(contato));
        when(reservaRepository.findByContatoIdAndSituacaoInAndDataGreaterThanEqualOrderByDataAsc(
                eq(100L), any(), any()
        )).thenReturn(List.of(reserva));
        when(interpretador.interpretar("Sim")).thenReturn(IntencaoResposta.CONFIRMA);

        boolean processou = servicoRespostaLembrete.processarPossivelResposta(
                empresa, numeroWhatsapp, "Sim", EstadoUsuario.INICIO
        );

        assertTrue(processou);
        verify(reservaRepository).save(reserva);
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(numeroWhatsapp), anyString());
    }

    @Test
    void deveSolicitarCancelamentoQuandoIntencaoForRemarca() {
        String chave = WhatsappUtil.normalizarParaChaveEstado(numeroWhatsapp);
        String indice = CriptografiaCampo.calcularIndice(chave);

        when(contatoRepository.findByEmpresaIdAndNumeroIndice(10L, indice))
                .thenReturn(Optional.of(contato));
        when(reservaRepository.findByContatoIdAndSituacaoInAndDataGreaterThanEqualOrderByDataAsc(
                eq(100L), any(), any()
        )).thenReturn(List.of(reserva));
        when(interpretador.interpretar("Cancelar")).thenReturn(IntencaoResposta.REMARCA);

        boolean processou = servicoRespostaLembrete.processarPossivelResposta(
                empresa, numeroWhatsapp, "Cancelar", EstadoUsuario.INICIO
        );

        assertTrue(processou);
        verify(reservaRepository).save(reserva);
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(numeroWhatsapp), anyString());
    }

    @Test
    void deveDesativarContatoQuandoIntencaoForParar() {
        String chave = WhatsappUtil.normalizarParaChaveEstado(numeroWhatsapp);
        String indice = CriptografiaCampo.calcularIndice(chave);

        when(contatoRepository.findByEmpresaIdAndNumeroIndice(10L, indice))
                .thenReturn(Optional.of(contato));
        when(reservaRepository.findByContatoIdAndSituacaoInAndDataGreaterThanEqualOrderByDataAsc(
                eq(100L), any(), any()
        )).thenReturn(List.of(reserva));
        when(interpretador.interpretar("parar")).thenReturn(IntencaoResposta.PARAR);

        boolean processou = servicoRespostaLembrete.processarPossivelResposta(
                empresa, numeroWhatsapp, "parar", EstadoUsuario.INICIO
        );

        assertTrue(processou);
        assertTrue(contato.isNaoContatar());
        verify(contatoRepository).save(contato);
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(numeroWhatsapp), anyString());
    }
}
