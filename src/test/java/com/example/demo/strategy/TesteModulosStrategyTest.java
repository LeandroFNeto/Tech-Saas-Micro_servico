package com.example.demo.strategy;

import com.example.demo.model.Empresa;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.strategy.experimental.ModuloExperimental;
import com.example.demo.strategy.experimental.RegistroModulosExperimentais;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TesteModulosStrategyTest {

    private static final String AUTORIZADO = "5511988887777@c.us";
    private static final String ESTRANHO = "5511900001111@c.us";

    @Mock private ServicoMensagem servicoMensagem;

    private final List<String> execucoes = new ArrayList<>();
    private TesteModulosStrategy estrategia;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        ModuloExperimental eco = new ModuloExperimental() {
            @Override
            public String nome() {
                return "eco";
            }

            @Override
            public String descricao() {
                return "Repete o texto recebido";
            }

            @Override
            public String responderSimulado(Empresa empresaDaSessao, String texto) {
                execucoes.add(texto);
                return "eco: " + texto;
            }
        };
        estrategia = new TesteModulosStrategy();
        ReflectionTestUtils.setField(estrategia, "servicoMensagem", servicoMensagem);
        ReflectionTestUtils.setField(estrategia, "registro", new RegistroModulosExperimentais(List.of(eco)));
        ReflectionTestUtils.setField(estrategia, "numerosAutorizados", "5511988887777, 5511977776666");

        empresa = new Empresa();
        empresa.setNome("Teste de módulos");
        empresa.setRamoDeAtuacao("TESTE_MODULOS");
    }

    private String ultimaResposta(String numero) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(servicoMensagem, atLeastOnce()).enviarMensagemWPP(org.mockito.ArgumentMatchers.eq(empresa),
                org.mockito.ArgumentMatchers.eq(numero), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Ramo da strategy é TESTE_MODULOS")
    void deveExporORamo() {
        assertThat(estrategia.getRamoDeAtuacao()).isEqualTo("TESTE_MODULOS");
    }

    @Test
    @DisplayName("Número autorizado lista os módulos registrados com nome e descrição")
    void deveListarModulosParaAutorizado() {
        estrategia.processarMensagem(empresa, AUTORIZADO, "/modulos", "INICIO");

        assertThat(ultimaResposta(AUTORIZADO)).contains("eco").contains("Repete o texto recebido");
        assertThat(execucoes).isEmpty();
    }

    @Test
    @DisplayName("Número autorizado executa o módulo e recebe a resposta simulada")
    void deveExecutarModuloParaAutorizado() {
        estrategia.processarMensagem(empresa, AUTORIZADO, "/modulo eco olá mundo", "INICIO");

        assertThat(ultimaResposta(AUTORIZADO)).isEqualTo("eco: olá mundo");
        assertThat(execucoes).containsExactly("olá mundo");
    }

    @Test
    @DisplayName("Módulo inexistente ou sem texto não executa nada")
    void deveRecusarModuloInexistenteOuSemTexto() {
        estrategia.processarMensagem(empresa, AUTORIZADO, "/modulo fantasma oi", "INICIO");
        assertThat(ultimaResposta(AUTORIZADO)).contains("Módulo não encontrado");

        estrategia.processarMensagem(empresa, AUTORIZADO, "/modulo eco", "INICIO");
        assertThat(ultimaResposta(AUTORIZADO)).contains("Informe o texto");
        assertThat(execucoes).isEmpty();
    }

    @Test
    @DisplayName("Mensagem sem comando devolve a ajuda dos comandos")
    void deveMostrarAjuda() {
        estrategia.processarMensagem(empresa, AUTORIZADO, "oi", "INICIO");

        assertThat(ultimaResposta(AUTORIZADO)).isEqualTo(TesteModulosStrategy.MENSAGEM_AJUDA);
    }

    @Test
    @DisplayName("Número não autorizado recebe a mensagem fixa e nenhum módulo roda")
    void deveBarrarNumeroNaoAutorizado() {
        estrategia.processarMensagem(empresa, ESTRANHO, "/modulo eco segredo", "INICIO");

        assertThat(ultimaResposta(ESTRANHO)).isEqualTo(TesteModulosStrategy.MENSAGEM_NAO_AUTORIZADO);
        assertThat(execucoes).isEmpty();
    }

    @Test
    @DisplayName("Lista de autorizados vazia avisa que não há número autorizado e nada roda")
    void deveAvisarComListaVazia() {
        ReflectionTestUtils.setField(estrategia, "numerosAutorizados", "");

        estrategia.processarMensagem(empresa, AUTORIZADO, "/modulo eco oi", "INICIO");

        assertThat(ultimaResposta(AUTORIZADO)).isEqualTo(TesteModulosStrategy.MENSAGEM_SEM_AUTORIZADOS);
        assertThat(execucoes).isEmpty();
        verify(servicoMensagem, never()).enviarMensagemWPP(empresa, AUTORIZADO, "eco: oi");
    }

    @Test
    @DisplayName("Lista de autorizados nula é tratada como vazia")
    void deveTratarListaNulaComoVazia() {
        ReflectionTestUtils.setField(estrategia, "numerosAutorizados", null);

        estrategia.processarMensagem(empresa, AUTORIZADO, "/modulos", "INICIO");

        assertThat(ultimaResposta(AUTORIZADO)).isEqualTo(TesteModulosStrategy.MENSAGEM_SEM_AUTORIZADOS);
        verify(servicoMensagem, never()).enviarMensagemWPP(org.mockito.ArgumentMatchers.any(), anyString(),
                org.mockito.ArgumentMatchers.contains("Módulos registrados"));
    }
}
