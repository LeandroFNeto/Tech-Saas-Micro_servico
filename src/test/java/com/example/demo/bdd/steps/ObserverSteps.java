package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObserverSteps {

    private static final String NUMERO = "5511999999999@c.us";
    private static final String OUTRO_NUMERO = "5511988888888@c.us";
    private static final long ESPERA_MAXIMA_MS = 20000;

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ServicoReserva servicoReserva;
    @Autowired private ServicoGoogleagenda servicoGoogleagenda;
    @Autowired private ServicoMensagem servicoMensagem;
    @Autowired private GerenciadorSessao gerenciadorSessao;
    @Autowired private BddContexto ctx;

    @Dado("a empresa persistida tem a agenda Google {string}")
    public void empresaComAgenda(String calendarId) {
        Empresa empresa = empresaRepository.findBySessaoWhatsapp(ctx.sessao);
        empresa.setGoogleCalendarId(calendarId);
        empresaRepository.save(empresa);
    }

    @E("a data está livre na agenda")
    public void dataLivre() throws Exception {
        when(servicoGoogleagenda.dataEstaLivre(anyString(), any())).thenReturn(true);
    }

    @E("a data está ocupada na agenda")
    public void dataOcupada() throws Exception {
        when(servicoGoogleagenda.dataEstaLivre(anyString(), any())).thenReturn(false);
    }

    @E("o cliente está com a reserva em registro")
    public void clienteRegistrando() {
        gerenciadorSessao.setEstado(NUMERO, EstadoUsuario.RESERVA_REGISTRANDO);
        gerenciadorSessao.getOuCriarReserva(NUMERO).setNomeCliente("Maria Silva");
    }

    @E("o Google responde 409 ao inserir o evento")
    public void googleResponde409() throws Exception {
        doThrow(erroGoogle(409, null)).when(servicoReserva).agendarNoGoogle(any(), any(), any(), any(), any());
    }

    @E("o Google falha sempre com o status {int} ao inserir o evento")
    public void googleFalhaSempre(int status) throws Exception {
        doThrow(erroGoogle(status, null)).when(servicoReserva).agendarNoGoogle(any(), any(), any(), any(), any());
    }

    @E("o Google recusa a primeira inserção com 403 por {string}")
    public void googleRecusaPrimeiraCom403(String motivo) throws Exception {
        doThrow(erroGoogle(403, motivo))
                .doReturn("evento-1")
                .when(servicoReserva).agendarNoGoogle(any(), any(), any(), any(), any());
    }

    @E("a credencial do Google está ausente")
    public void credencialAusente() throws Exception {
        doThrow(new IllegalStateException("credencial ausente"))
                .when(servicoReserva).agendarNoGoogle(any(), any(), any(), any(), any());
    }

    @E("a primeira tentativa chega ao Google e a resposta se perde")
    public void respostaSePerde() throws Exception {
        when(servicoGoogleagenda.dataEstaLivre(anyString(), any())).thenReturn(true, false);
        doThrow(new IOException("resposta perdida"))
                .when(servicoReserva).agendarNoGoogle(any(), any(), any(), any(), any());
        when(servicoReserva.eventoDoPedidoExiste(any(), any())).thenReturn(true);
    }

    @E("o envio da mensagem ao cliente falha")
    public void envioFalha() {
        doThrow(new RuntimeException("WPPConnect fora do ar"))
                .when(servicoMensagem).enviarMensagemWPP(any(), anyString(), anyString());
    }

    @E("o Google guarda o evento assim que ele é inserido")
    public void googleGuardaEvento() throws Exception {
        AtomicBoolean reservado = new AtomicBoolean(false);
        when(servicoGoogleagenda.dataEstaLivre(anyString(), any())).thenAnswer(invocacao -> !reservado.get());
        doAnswer(invocacao -> {
            Thread.sleep(300);
            reservado.set(true);
            return "evento-1";
        }).when(servicoReserva).agendarNoGoogle(any(), any(), any(), any(), any());
    }

    @Quando("o evento EventoReservaConfirmada é publicado para a data {string}")
    public void publicaEvento(String data) {
        Empresa empresa = empresaRepository.findBySessaoWhatsapp(ctx.sessao);
        long inicio = System.nanoTime();
        publisher.publishEvent(new EventoReservaConfirmada(
                empresa, data, "Diária Completa", "Maria Silva", "5511999999999@c.us", "pedido-teste"));
        long bloqueioMs = Duration.ofNanos(System.nanoTime() - inicio).toMillis();
        ctx.status = (int) bloqueioMs;
    }

    @Quando("duas confirmações da mesma data {string} são publicadas")
    public void duasConfirmacoes(String data) {
        gerenciadorSessao.setEstado(NUMERO, EstadoUsuario.RESERVA_REGISTRANDO);
        gerenciadorSessao.setEstado(OUTRO_NUMERO, EstadoUsuario.RESERVA_REGISTRANDO);
        Empresa empresa = empresaRepository.findBySessaoWhatsapp(ctx.sessao);
        publisher.publishEvent(new EventoReservaConfirmada(
                empresa, data, "Diária Completa", "Maria Silva", NUMERO, "pedido-a"));
        publisher.publishEvent(new EventoReservaConfirmada(
                empresa, data, "Diária Completa", "Joana Prado", OUTRO_NUMERO, "pedido-b"));
    }

    @Então("o ObservadorGoogleAgenda deve agendar no Google sem bloquear o publicador")
    public void observerAgendou() throws Exception {
        assertPublicacaoNaoBloqueou();
        verify(servicoReserva, timeout(3000).atLeastOnce())
                .agendarNoGoogle(any(Empresa.class), eq("25-12-2026"), eq("Maria Silva"), eq("5511999999999@c.us"), eq("pedido-teste"));
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(servicoReserva, atLeastOnce())
                        .agendarNoGoogle(any(Empresa.class), eq("25-12-2026"), eq("Maria Silva"), eq("5511999999999@c.us"), eq("pedido-teste")));
    }

    @Então("o cliente recebe a mensagem que contém {string}")
    public void clienteRecebe(String trecho) {
        verify(servicoMensagem, timeout(ESPERA_MAXIMA_MS).atLeastOnce())
                .enviarMensagemWPP(any(Empresa.class), eq(NUMERO), contains(trecho));
    }

    @Então("uma só confirmação e um só aviso de data ocupada são enviados")
    public void umaConfirmacaoEUmAviso() {
        verify(servicoMensagem, timeout(ESPERA_MAXIMA_MS).times(1))
                .enviarMensagemWPP(any(Empresa.class), anyString(), contains("Reserva confirmada"));
        verify(servicoMensagem, timeout(ESPERA_MAXIMA_MS).times(1))
                .enviarMensagemWPP(any(Empresa.class), anyString(), contains("acabou de ser reservada"));
    }

    @E("o cliente não recebe a mensagem que contém {string}")
    public void clienteNaoRecebe(String trecho) {
        verify(servicoMensagem, never()).enviarMensagemWPP(any(Empresa.class), eq(NUMERO), contains(trecho));
    }

    @E("o Google recebe {int} tentativa(s) de inserção")
    public void tentativasDeInsercao(int quantidade) throws Exception {
        verify(servicoReserva, times(quantidade)).agendarNoGoogle(any(), any(), any(), any(), any());
    }

    @E("a conversa do cliente deve estar em {string}")
    public void conversaEm(String estado) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                org.assertj.core.api.Assertions.assertThat(gerenciadorSessao.getEstado(NUMERO).name()).isEqualTo(estado));
    }

    @E("a agenda aceita o horário em todas as datas")
    public void horarioLivreEmTodasAsDatas() throws Exception {
        when(servicoGoogleagenda.horarioEstaLivre(anyString(), any(), any(), any(), anyInt())).thenReturn(true);
    }

    @E("o horário está ocupado em {string}")
    public void horarioOcupadoEm(String data) throws Exception {
        when(servicoGoogleagenda.horarioEstaLivre(
                anyString(), eq(java.time.LocalDate.parse(data, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))),
                any(), any(), anyInt())).thenReturn(false);
    }

    @E("o Google falha sempre com o status {int} ao inserir o evento por hora")
    public void googleFalhaSempreNaInsercaoPorHora(int status) throws Exception {
        doThrow(erroGoogle(status, null)).when(servicoReserva)
                .agendarPorHora(any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Quando("o evento por hora é publicado para as datas {string} às {string}")
    public void publicaEventoPorHora(String datas, String horario) {
        Empresa empresa = empresaRepository.findBySessaoWhatsapp(ctx.sessao);
        gerenciadorSessao.setEstado(NUMERO, EstadoUsuario.RESERVA_REGISTRANDO);
        long inicio = System.nanoTime();
        publisher.publishEvent(new EventoReservaConfirmada(
                empresa, List.of(datas.split(",")), horario, "Maria Silva", NUMERO, "pedido-hora", true));
        ctx.status = (int) Duration.ofNanos(System.nanoTime() - inicio).toMillis();
    }

    @E("o Google recebe {int} inserção/inserções por hora")
    public void insercoesPorHora(int quantidade) throws Exception {
        verify(servicoReserva, timeout(ESPERA_MAXIMA_MS).times(quantidade))
                .agendarPorHora(any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @E("o Google remove {int} evento(s) do pedido por hora")
    public void remocoesPorHora(int quantidade) throws Exception {
        verify(servicoReserva, timeout(ESPERA_MAXIMA_MS).times(quantidade))
                .removerEventoPorHora(any(), any(), anyInt());
    }

    private void assertPublicacaoNaoBloqueou() {
        org.assertj.core.api.Assertions.assertThat(ctx.status)
                .as("publishEvent não deve esperar o @Async do Observer")
                .isLessThan(200);
    }

    private static GoogleJsonResponseException erroGoogle(int status, String motivo) {
        GoogleJsonError erro = new GoogleJsonError();
        erro.setCode(status);
        if (motivo != null) {
            GoogleJsonError.ErrorInfo info = new GoogleJsonError.ErrorInfo();
            info.setReason(motivo);
            erro.setErrors(List.of(info));
        }
        return new GoogleJsonResponseException(
                new HttpResponseException.Builder(status, "erro do Google", new HttpHeaders()), erro);
    }
}
