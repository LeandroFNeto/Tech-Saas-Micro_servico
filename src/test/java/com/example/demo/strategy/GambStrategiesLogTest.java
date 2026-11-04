package com.example.demo.strategy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.model.Empresa;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.LimiteDiarioMensagens;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import com.example.demo.strategy.experimental.RegistroModulosExperimentais;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GambStrategiesLogTest {

    private static final String CLIENTE = "5511988887777@c.us";
    private static final String TEXTO_LIVRE = "Quero falar do contrato sigiloso da Fulano Ltda";
    private static final String EMAIL = "responsavel.secreto@gamb.com.br";

    @Mock private ServicoMensagem servicoMensagem;
    @Mock private ServicoGoogleagenda servicoGoogleagenda;
    @Mock private ServicoFuncionamento servicoFuncionamento;
    @Mock private ServicoReserva servicoReserva;
    @Mock private LimiteDiarioMensagens limiteDiarioMensagens;
    @Mock private AvisoRepository avisoRepository;
    @Mock private ServicoIA servicoIA;
    @Spy private GerenciadorSessao gerenciadorSessao = new GerenciadorSessao();
    @InjectMocks private AtendenteGambStrategy atendente;

    private final TesteModulosStrategy teste = new TesteModulosStrategy();
    private final ListAppender<ILoggingEvent> captura = new ListAppender<>();
    private Logger loggerAtendente;
    private Logger loggerTeste;
    private Level nivelAtendente;
    private Level nivelTeste;

    @BeforeEach
    void iniciarCaptura() {
        ReflectionTestUtils.setField(atendente, "assistenteTutoriaService",
                new AssistenteTutoriaService(avisoRepository, servicoIA));
        when(limiteDiarioMensagens.registrarSePermitido(anyString())).thenReturn(true);
        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of());

        ReflectionTestUtils.setField(teste, "servicoMensagem", servicoMensagem);
        ReflectionTestUtils.setField(teste, "registro", new RegistroModulosExperimentais(List.of()));
        ReflectionTestUtils.setField(teste, "numerosAutorizados", "5511988887777");

        loggerAtendente = (Logger) LoggerFactory.getLogger(AtendenteGambStrategy.class);
        loggerTeste = (Logger) LoggerFactory.getLogger(TesteModulosStrategy.class);
        nivelAtendente = loggerAtendente.getLevel();
        nivelTeste = loggerTeste.getLevel();
        loggerAtendente.setLevel(Level.DEBUG);
        loggerTeste.setLevel(Level.DEBUG);
        captura.start();
        loggerAtendente.addAppender(captura);
        loggerTeste.addAppender(captura);
    }

    @AfterEach
    void encerrarCaptura() {
        loggerAtendente.detachAppender(captura);
        loggerTeste.detachAppender(captura);
        captura.stop();
        loggerAtendente.setLevel(nivelAtendente);
        loggerTeste.setLevel(nivelTeste);
    }

    private List<String> linhas() {
        return captura.list.stream()
                .map(evento -> evento.getFormattedMessage() + " "
                        + (evento.getArgumentArray() == null ? "" : String.join(" ",
                        Arrays.stream(evento.getArgumentArray()).map(String::valueOf).toList())))
                .toList();
    }

    @Test
    @DisplayName("Atendente: número sai mascarado e nem o texto nem o e-mail dos responsáveis aparecem no log")
    void atendenteNaoVazaNumeroTextoNemEmail() {
        Empresa empresa = new Empresa();
        empresa.setGoogleCalendarId("gamb@group.calendar.google.com");
        empresa.setEmailsResponsaveisReuniao(List.of(EMAIL));

        atendente.processarMensagem(empresa, CLIENTE, "oi", "INICIO");
        atendente.processarMensagem(empresa, CLIENTE, "1", "ATENDENTE_MENU");
        atendente.processarMensagem(empresa, CLIENTE, TEXTO_LIVRE, "ATENDENTE_DUVIDA");

        assertThat(linhas()).isNotEmpty();
        assertThat(linhas()).anyMatch(linha -> linha.contains("***7777"));
        assertThat(linhas()).noneMatch(linha -> linha.contains("5511988887777")
                || linha.contains("Fulano") || linha.contains("contrato sigiloso") || linha.contains(EMAIL));
    }

    @Test
    @DisplayName("Bot de teste: número sai mascarado e o texto do comando não aparece no log")
    void testeNaoVazaNumeroNemTexto() {
        Empresa empresa = new Empresa();

        teste.processarMensagem(empresa, CLIENTE, "/modulo eco " + TEXTO_LIVRE, "INICIO");
        teste.processarMensagem(empresa, "5511900001111@c.us", "/modulos", "INICIO");

        assertThat(linhas()).anyMatch(linha -> linha.contains("***7777"));
        assertThat(linhas()).anyMatch(linha -> linha.contains("***1111"));
        assertThat(linhas()).noneMatch(linha -> linha.contains("5511988887777") || linha.contains("5511900001111")
                || linha.contains("Fulano") || linha.contains("contrato sigiloso"));
    }
}
