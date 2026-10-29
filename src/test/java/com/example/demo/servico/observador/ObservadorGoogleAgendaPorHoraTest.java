package com.example.demo.servico.observador;

import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservadorGoogleAgendaPorHoraTest {

    private static final String NUMERO = "5511999997777@c.us";
    private static final String PRIMEIRA = "10-11-2026";
    private static final String SEGUNDA = "17-11-2026";
    private static final ServicoFuncionamento.Janela JANELA =
            new ServicoFuncionamento.Janela(true, LocalTime.of(8, 0), LocalTime.of(22, 0), 60, null);

    private ServicoReserva servicoReserva;
    private ServicoMensagem servicoMensagem;
    private ServicoGoogleagenda servicoGoogleagenda;
    private ServicoFuncionamento servicoFuncionamento;
    private GerenciadorSessao gerenciadorSessao;
    private ObservadorGoogleAgenda observador;
    private Empresa empresa;

    @BeforeEach
    void preparar() throws Exception {
        servicoReserva = mock(ServicoReserva.class);
        servicoMensagem = mock(ServicoMensagem.class);
        servicoGoogleagenda = mock(ServicoGoogleagenda.class);
        servicoFuncionamento = mock(ServicoFuncionamento.class);
        gerenciadorSessao = mock(GerenciadorSessao.class);
        observador = new ObservadorGoogleAgenda() {
            @Override
            protected void esperar(long milissegundos) {
            }
        };
        ReflectionTestUtils.setField(observador, "servicoReserva", servicoReserva);
        ReflectionTestUtils.setField(observador, "servicoMensagem", servicoMensagem);
        ReflectionTestUtils.setField(observador, "servicoGoogleagenda", servicoGoogleagenda);
        ReflectionTestUtils.setField(observador, "servicoFuncionamento", servicoFuncionamento);
        ReflectionTestUtils.setField(observador, "gerenciadorSessao", gerenciadorSessao);

        empresa = new Empresa();
        empresa.setId(42L);
        empresa.setGoogleCalendarId("agenda@group.calendar.google.com");
        when(servicoFuncionamento.janelaDa(eq(empresa), any())).thenReturn(JANELA);
        when(servicoGoogleagenda.horarioEstaLivre(anyString(), any(), any(), any(), anyInt())).thenReturn(true);
    }

    private EventoReservaConfirmada evento(String... datas) {
        return new EventoReservaConfirmada(empresa, List.of(datas), "14:00", "Joana Prado", NUMERO, "pedido-1", true);
    }

    @Test
    @DisplayName("Registra um evento com hora para cada data e confirma com as datas e o horário")
    void deveCriarUmEventoPorData() throws Exception {
        observador.registarNoGoogle(evento(PRIMEIRA, SEGUNDA));

        verify(servicoReserva).agendarPorHora(eq(empresa), eq(LocalDate.of(2026, 11, 10)),
                eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)), eq("Joana Prado"), eq(NUMERO), eq("pedido-1"), eq(0));
        verify(servicoReserva).agendarPorHora(eq(empresa), eq(LocalDate.of(2026, 11, 17)),
                eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)), eq("Joana Prado"), eq(NUMERO), eq("pedido-1"), eq(1));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains("Datas: " + PRIMEIRA + ", " + SEGUNDA));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains("14:00"));
        verify(gerenciadorSessao).concluirRegistro(NUMERO, EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Uma data ocupada na reconsulta: nada é criado e o cliente sabe qual caiu")
    void naoDeveCriarNadaQuandoUmaDataEstaOcupada() throws Exception {
        when(servicoGoogleagenda.horarioEstaLivre(anyString(), eq(LocalDate.of(2026, 11, 17)), any(), any(), anyInt()))
                .thenReturn(false);
        DadosReservaSessao reserva = new DadosReservaSessao();
        reserva.setHorario("14:00");
        reserva.adicionarData(LocalDate.of(2026, 11, 10));
        reserva.adicionarData(LocalDate.of(2026, 11, 17));
        when(gerenciadorSessao.getReserva(NUMERO)).thenReturn(reserva);

        observador.registarNoGoogle(evento(PRIMEIRA, SEGUNDA));

        verify(servicoReserva, never()).agendarPorHora(any(), any(), any(), any(), any(), any(), any(), anyInt());
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains(SEGUNDA));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains("Nada foi reservado"));
        verify(gerenciadorSessao).concluirRegistro(NUMERO, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        assertThat(reserva.getDatas()).containsExactly(LocalDate.of(2026, 11, 10));
    }

    @Test
    @DisplayName("Data que passou a ser fechada pela empresa também derruba só ela")
    void deveRecusarDataQueFechouNoMeioDoCaminho() throws Exception {
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(LocalDate.of(2026, 11, 17))))
                .thenReturn(ServicoFuncionamento.Janela.fechada("folga"));

        observador.registarNoGoogle(evento(PRIMEIRA, SEGUNDA));

        verify(servicoReserva, never()).agendarPorHora(any(), any(), any(), any(), any(), any(), any(), anyInt());
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains(SEGUNDA));
        verify(gerenciadorSessao).concluirRegistro(NUMERO, EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Falha de rede na segunda data: a nova tentativa não repete o evento que já existe")
    void deveRetomarSemDuplicarOEventoJaCriado() throws Exception {
        doReturn(true).when(servicoReserva).eventoPorHoraExiste(eq(empresa), eq("pedido-1"), eq(0));
        doReturn(false).when(servicoReserva).eventoPorHoraExiste(eq(empresa), eq("pedido-1"), eq(1));
        doThrow(new IOException("rede")).doReturn("id")
                .when(servicoReserva).agendarPorHora(any(), eq(LocalDate.of(2026, 11, 17)), any(), any(), any(), any(), any(), eq(1));

        observador.registarNoGoogle(evento(PRIMEIRA, SEGUNDA));

        verify(servicoReserva, times(1)).agendarPorHora(any(), eq(LocalDate.of(2026, 11, 10)), any(), any(), any(), any(), any(), eq(0));
        verify(servicoReserva, times(2)).agendarPorHora(any(), eq(LocalDate.of(2026, 11, 17)), any(), any(), any(), any(), any(), eq(1));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains("Reserva confirmada"));
        verify(gerenciadorSessao).concluirRegistro(NUMERO, EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Falha definitiva no meio: apaga os eventos do pedido e avisa o cliente")
    void deveCompensarQuandoAInsercaoFalhaDeVez() throws Exception {
        doThrow(new IOException("rede")).when(servicoReserva)
                .agendarPorHora(any(), eq(LocalDate.of(2026, 11, 17)), any(), any(), any(), any(), any(), eq(1));

        observador.registarNoGoogle(evento(PRIMEIRA, SEGUNDA));

        verify(servicoReserva).removerEventoPorHora(empresa, "pedido-1", 0);
        verify(servicoReserva).removerEventoPorHora(empresa, "pedido-1", 1);
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains("Não consegui registrar sua reserva"));
        verify(gerenciadorSessao).concluirRegistro(NUMERO, EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Compensação que falha não derruba o fluxo e a conversa volta ao início")
    void deveSeguirQuandoACompensacaoFalha() throws Exception {
        doThrow(new IOException("rede")).when(servicoReserva)
                .agendarPorHora(any(), any(), any(), any(), any(), any(), any(), anyInt());
        doThrow(new IOException("rede")).when(servicoReserva).removerEventoPorHora(any(), any(), anyInt());

        observador.registarNoGoogle(evento(PRIMEIRA, SEGUNDA));

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(NUMERO), contains("Não consegui registrar sua reserva"));
        verify(gerenciadorSessao).concluirRegistro(NUMERO, EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("O intervalo da empresa entra na checagem de conflito")
    void deveUsarOIntervaloDaEmpresa() throws Exception {
        empresa.setIntervaloEntreAtendimentosMinutos(30);

        observador.registarNoGoogle(evento(PRIMEIRA));

        verify(servicoGoogleagenda).horarioEstaLivre(anyString(), eq(LocalDate.of(2026, 11, 10)),
                eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)), eq(30));
    }
}
