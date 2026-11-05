package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.servico.observador.Observadorwhatsapp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ServicoMensagemTest {

    private static final String CONTATO = "5511988887777@c.us";

    private ServicoMensagem servico;
    private RegistroEnviosBot registro;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        registro = new RegistroEnviosBot();
        servico = spy(new ServicoMensagem());
        ReflectionTestUtils.setField(servico, "registroEnviosBot", registro);
        ReflectionTestUtils.setField(servico, "observador", mock(Observadorwhatsapp.class));
        ReflectionTestUtils.setField(servico, "wppConnectService", mock(ServicoWppConnect.class));
        empresa = new Empresa();
        empresa.setSessaoWhatsapp("sessao");
    }

    @Test
    @DisplayName("O envio do bot fica registrado antes de o gateway responder, para o eco não ligar a trava")
    void registraAntesDeEnviar() {
        AtomicBoolean registradoDuranteOEnvio = new AtomicBoolean();
        doAnswer(invocacao -> {
            registradoDuranteOEnvio.set(registro.foiEnviadoPeloBot(CONTATO, "Olá!"));
            return null;
        }).when(servico).postWpp(eq("sessao"), eq("/send-message"), anyMap());

        servico.enviarMensagemWPP(empresa, CONTATO, "Olá!");

        assertThat(registradoDuranteOEnvio).isTrue();
        assertThat(registro.foiEnviadoPeloBot(CONTATO, "Olá!")).isTrue();
    }

    @Test
    @DisplayName("Se o envio falhar, o registro é desfeito")
    void desfazORegistroQuandoOEnvioFalha() {
        doThrow(new RuntimeException("falha")).when(servico).postWpp(anyString(), anyString(), anyMap());

        servico.enviarMensagemWPP(empresa, CONTATO, "Olá!");

        assertThat(registro.foiEnviadoPeloBot(CONTATO, "Olá!")).isFalse();
    }

    @Test
    @DisplayName("Imagem também é registrada antes do envio e desfeita se falhar")
    void registraImagemAntesDeEnviar() {
        AtomicBoolean registradoDuranteOEnvio = new AtomicBoolean();
        doAnswer(invocacao -> {
            registradoDuranteOEnvio.set(registro.foiEnviadoPeloBot(CONTATO, "legenda"));
            throw new RuntimeException("falha");
        }).when(servico).postWpp(eq("sessao"), eq("/send-image"), anyMap());

        servico.enviarImagemWPP(empresa, CONTATO, "https://x.com/a.png", "legenda");

        assertThat(registradoDuranteOEnvio).isTrue();
        assertThat(registro.foiEnviadoPeloBot(CONTATO, "legenda")).isFalse();
    }
}
