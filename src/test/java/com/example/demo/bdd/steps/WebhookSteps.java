package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.strategy.FactoryModulo;
import io.cucumber.datatable.DataTable;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.cucumber.java.After;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import org.slf4j.LoggerFactory;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class WebhookSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private BddContexto ctx;
    @Autowired private Observadorwhatsapp observador;
    @Autowired private FactoryModulo factoryModulo;
    @Autowired private ServicoMensagem servicoMensagem;

    private ListAppender<ILoggingEvent> capturaLog;

    @Dado("que a saída de log está sendo capturada")
    public void capturarLog() {
        Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        capturaLog = new ListAppender<>();
        capturaLog.start();
        raiz.addAppender(capturaLog);
    }

    @After
    public void pararCapturaLog() {
        if (capturaLog != null) {
            Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            raiz.detachAppender(capturaLog);
            capturaLog.stop();
            capturaLog = null;
        }
    }

    @E("a saída de log deve conter {string}")
    public void logContem(String trecho) {
        assertThat(textoCapturado()).contains(trecho);
    }

    @E("a saída de log não deve conter {string}")
    public void logNaoContem(String trecho) {
        assertThat(textoCapturado()).doesNotContain(trecho);
    }

    private String textoCapturado() {
        return capturaLog.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    @Quando("eu envio POST \\/webhook\\/whatsapp com o WhatsappWebhookDTO:")
    public void postWebhook(DataTable tabela) throws Exception {
        String json = json(tabela);
        MvcResult resultado = mockMvc.perform(post("/webhook/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }

    @E("o observador deve registrar sucesso da sessão {string}")
    public void observadorSucesso(String sessao) {
        verify(observador, atLeastOnce()).logSucesso(eq(sessao), anyString(), anyString());
    }

    @E("o observador deve registrar filtro contendo {string}")
    public void observadorFiltro(String trecho) {
        verify(observador, atLeastOnce()).logFiltro(any(), any(), contains(trecho));
    }

    @E("o observador deve registrar erro contendo {string}")
    public void observadorErro(String trecho) {
        verify(observador, atLeastOnce()).logErro(any(), any(), eq("ControllerWhatsapp"), any(Exception.class));
    }

    @E("a FactoryModulo não deve ser consultada")
    public void factoryNaoConsultada() {
        verify(factoryModulo, never()).obterEstrategia(any());
    }

    @E("a FactoryModulo deve ter resolvido o ramo {string}")
    public void factoryResolveu(String ramo) {
        verify(factoryModulo).obterEstrategia(ramo);
    }

    @E("a LocacaoStrategy deve ter processado a mensagem")
    public void locacaoProcessou() {
        verify(servicoMensagem, atLeastOnce()).enviarMensagemWPP(any(), anyString(), anyString());
    }

    private String json(DataTable tabela) {
        Map<String, String> mapa = tabela.asMap();
        return mapa.entrySet().stream()
                .map(e -> {
                    String valor = e.getValue();
                    if ("true".equals(valor) || "false".equals(valor) || valor.matches("-?\\d+(\\.\\d+)?")) {
                        return "\"" + e.getKey() + "\":" + valor;
                    }
                    return "\"" + e.getKey() + "\":\"" + valor.replace("\"", "\\\"") + "\"";
                })
                .collect(Collectors.joining(",", "{", "}"));
    }
}
