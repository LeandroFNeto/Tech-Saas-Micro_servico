package com.example.demo.strategy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoMenu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocacaoStrategyLogTest {

    private static final String REMETENTE = "5511988887777@c.us";
    private static final String TEXTO_LIVRE = "12/05 meu nome e Fulano Sigiloso";

    @Mock private ServicoMensagem servicoMensagem;
    @Spy private ServicoMenu servicoMenu = new ServicoMenu();
    @Mock private ServicoGoogleagenda servicoGoogleagenda;
    @Spy private GerenciadorSessao gerenciadorSessao = new GerenciadorSessao();
    @InjectMocks private LocacaoStrategy locacaoStrategy;

    private Logger logger;
    private Level nivelOriginal;
    private ListAppender<ILoggingEvent> captura;

    @BeforeEach
    void iniciarCaptura() {
        logger = (Logger) LoggerFactory.getLogger(LocacaoStrategy.class);
        nivelOriginal = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        captura = new ListAppender<>();
        captura.start();
        logger.addAppender(captura);
    }

    @AfterEach
    void encerrarCaptura() {
        logger.detachAppender(captura);
        captura.stop();
        logger.setLevel(nivelOriginal);
    }

    @Test
    @DisplayName("Texto livre na pesquisa de data não vai para o log, só a frase fixa")
    void textoDigitadoNaPesquisaDeDataNaoApareceNoLog() {
        Empresa empresa = new Empresa();
        empresa.setNome("Recanto Teste");
        empresa.setRamoDeAtuacao("LOCACAO");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, TEXTO_LIVRE, "ESPERANDO_DATA");

        List<String> mensagens = captura.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(mensagens).contains("Buscando data informada pelo cliente.");
        assertThat(mensagens).noneMatch(m -> m.contains("Fulano") || m.contains("12/05") || m.contains(TEXTO_LIVRE));
        assertThat(captura.list).noneMatch(e -> e.getArgumentArray() != null
                && java.util.Arrays.stream(e.getArgumentArray()).anyMatch(a -> String.valueOf(a).contains("Fulano")));
        verify(servicoMensagem).enviarMensagemWPP(any(), any(), any());
    }
}
