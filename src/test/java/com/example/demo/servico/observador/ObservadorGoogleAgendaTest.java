package com.example.demo.servico.observador;

import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.model.Empresa;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class ObservadorGoogleAgendaTest {

    private static final String NOME_CLIENTE = "Joana Prado";
    private static final String NUMERO = "5511999997777@c.us";

    private Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setId(42L);
        return empresa;
    }

    private ObservadorGoogleAgenda observador(ServicoReserva reserva, ServicoMensagem mensagem) {
        ObservadorGoogleAgenda observador = new ObservadorGoogleAgenda();
        ReflectionTestUtils.setField(observador, "servicoReserva", reserva);
        ReflectionTestUtils.setField(observador, "servicoMensagem", mensagem);
        return observador;
    }

    @Test
    @DisplayName("Falha no Google registra só o id da empresa e a classe, sem mensagem nem nome do cliente")
    void falhaNaoVazaMensagemNemNome(CapturedOutput saida) throws Exception {
        ServicoReserva reserva = mock(ServicoReserva.class);
        ServicoMensagem mensagem = mock(ServicoMensagem.class);
        doThrow(new IllegalStateException("Erro ao gravar evento de " + NOME_CLIENTE + " na agenda secreta@gmail.com"))
                .when(reserva).agendarNoGoogle(any(), anyString(), anyString());

        observador(reserva, mensagem).registarNoGoogle(
                new EventoReservaConfirmada(empresa(), "2026-12-01", NOME_CLIENTE, NUMERO));

        assertThat(saida.getAll())
                .contains("empresa 42")
                .contains("IllegalStateException")
                .doesNotContain(NOME_CLIENTE)
                .doesNotContain("Erro ao gravar evento")
                .doesNotContain("secreta@gmail.com")
                .doesNotContain("5511999997777");
        verify(mensagem).enviarMensagemWPP(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Sucesso também não escreve o nome do cliente nem o número")
    void sucessoNaoVazaNomeNemNumero(CapturedOutput saida) {
        ServicoReserva reserva = mock(ServicoReserva.class);
        ServicoMensagem mensagem = mock(ServicoMensagem.class);

        observador(reserva, mensagem).registarNoGoogle(
                new EventoReservaConfirmada(empresa(), "2026-12-01", NOME_CLIENTE, NUMERO));

        assertThat(saida.getAll()).contains("empresa 42").doesNotContain(NOME_CLIENTE).doesNotContain("5511999997777");
    }
}
