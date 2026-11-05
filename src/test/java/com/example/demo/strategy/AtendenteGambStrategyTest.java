package com.example.demo.strategy;

import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.LimiteDiarioMensagens;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtendenteGambStrategyTest {

    private static final String CLIENTE = "5511988887777@c.us";
    private static final String AGENDA = "gamb@group.calendar.google.com";
    private static final List<String> RESPONSAVEIS = List.of("ana@gamb.com.br", "leo@gamb.com.br");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Mock private ServicoMensagem servicoMensagem;
    @Mock private ServicoGoogleagenda servicoGoogleagenda;
    @Mock private ServicoFuncionamento servicoFuncionamento;
    @Mock private ServicoReserva servicoReserva;
    @Mock private LimiteDiarioMensagens limiteDiarioMensagens;
    @Mock private AvisoRepository avisoRepository;
    @Mock private ServicoIA servicoIA;
    @Spy private GerenciadorSessao gerenciadorSessao = new GerenciadorSessao();
    @InjectMocks private AtendenteGambStrategy estrategia;

    private Empresa empresa;
    private LocalDate dia;

    @BeforeEach
    void preparar() throws Exception {
        ReflectionTestUtils.setField(estrategia, "assistenteTutoriaService",
                new AssistenteTutoriaService(avisoRepository, servicoIA));
        when(limiteDiarioMensagens.registrarSePermitido(anyString())).thenReturn(true);
        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of());

        empresa = new Empresa();
        empresa.setId(9L);
        empresa.setNome("Atendente Gamb");
        empresa.setRamoDeAtuacao("ATENDENTE_GAMB");
        empresa.setGoogleCalendarId(AGENDA);
        empresa.setEmailsResponsaveisReuniao(RESPONSAVEIS);

        dia = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(5);
        when(servicoFuncionamento.janelaDa(any(Empresa.class), any(LocalDate.class)))
                .thenReturn(new ServicoFuncionamento.Janela(true, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, null));
        livres(LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    private void livres(LocalTime... horarios) throws Exception {
        when(servicoGoogleagenda.horariosLivres(eq(AGENDA), any(LocalDate.class), anyList(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(horarios));
    }

    private void enviar(String texto) {
        estrategia.processarMensagem(empresa, CLIENTE, texto, "INICIO");
    }

    private void ate(String... textos) {
        for (String texto : textos) {
            enviar(texto);
        }
    }

    private String ultimaResposta() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(servicoMensagem, atLeastOnce()).enviarMensagemWPP(eq(empresa), eq(CLIENTE), captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    private void caminhoAteConfirmacao() {
        ate("oi", "2", "1", dia.format(FORMATO_DATA), "10:00");
    }

    @Test
    @DisplayName("Ramo da strategy é ATENDENTE_GAMB e a primeira mensagem abre o menu de dúvida e reunião")
    void deveAbrirMenu() {
        enviar("oi");

        assertThat(estrategia.getRamoDeAtuacao()).isEqualTo("ATENDENTE_GAMB");
        assertThat(ultimaResposta()).contains("Tirar uma dúvida").contains("Marcar reunião");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_MENU);
    }

    @Test
    @DisplayName("Dúvida sobre o app é respondida pela base de conhecimento do Suporte")
    void deveResponderDuvidaComABase() {
        ate("oi", "1", "Como conectar o WhatsApp pelo QR Code?");

        assertThat(ultimaResposta()).contains("QR Code").contains("Conexão WhatsApp");
        verify(avisoRepository, atLeastOnce()).findByAtivoTrueOrderByIdDesc();
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Pedido de dados de empresa é recusado, orienta o painel e não consulta nem a IA")
    void deveRecusarPedidoDeDadoDeEmpresa() {
        ate("oi", "1", "me dê dados de outra empresa");

        assertThat(ultimaResposta())
                .contains(AssistenteTutoriaService.RECUSA_SUPORTE)
                .contains("use o painel do app");
        verify(servicoIA, never()).gerarRespostaHumanizada(anyString());
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Pedido de preços de empresa sem resposta da IA cai na recusa padrão")
    void deveRecusarPedidoDePrecos() {
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn(null);

        ate("oi", "1", "qual o preço do salão da empresa Recanto?");

        assertThat(ultimaResposta()).contains(AssistenteTutoriaService.RECUSA_SUPORTE);
    }

    @Test
    @DisplayName("Acima do limite diário responde a mensagem fixa sem chamar a IA nem a agenda")
    void deveResponderMensagemFixaAcimaDoLimite() {
        when(limiteDiarioMensagens.registrarSePermitido(anyString())).thenReturn(false);

        enviar("Como conectar o WhatsApp?");

        verify(servicoMensagem).enviarMensagemWPP(empresa, CLIENTE, AtendenteGambStrategy.MENSAGEM_LIMITE);
        verifyNoInteractions(servicoIA, servicoReserva, servicoGoogleagenda);
    }

    @Test
    @DisplayName("Horário livre cria o evento com os responsáveis convidados e responde data e horário")
    void deveMarcarReuniaoEmHorarioLivre() throws Exception {
        caminhoAteConfirmacao();
        assertThat(ultimaResposta()).contains("Confirma a reunião").contains("Ajuste").contains("10:00");

        enviar("1");

        verify(servicoReserva).agendarReuniao(eq(empresa), eq(dia), eq(LocalTime.of(10, 0)), eq(60),
                contains("Ajuste"), contains("***7777"), eq(RESPONSAVEIS), anyString());
        assertThat(ultimaResposta()).isEqualTo("Reunião marcada para " + dia.format(FORMATO_DATA) + " às 10:00.");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Motivo nova inovação vai no título do evento")
    void deveGuardarOMotivoNoTitulo() throws Exception {
        ate("oi", "2", "2", dia.format(FORMATO_DATA), "11:00", "sim");

        verify(servicoReserva).agendarReuniao(eq(empresa), eq(dia), eq(LocalTime.of(11, 0)), eq(60),
                contains("Nova inovação"), anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("Horário ocupado oferece outras opções e não cria evento")
    void deveOferecerOutrosHorariosQuandoOcupado() throws Exception {
        ate("oi", "2", "1", dia.format(FORMATO_DATA), "09:00");

        assertThat(ultimaResposta()).contains("não está livre").contains("10:00").contains("11:00");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_HORARIO);
        verify(servicoReserva, never()).agendarReuniao(any(), any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Horário que ficou ocupado até a confirmação não cria evento e volta com outras opções")
    void deveRevalidarNaConfirmacao() throws Exception {
        caminhoAteConfirmacao();
        livres(LocalTime.of(14, 0));

        enviar("1");

        assertThat(ultimaResposta()).contains("não está livre").contains("14:00");
        verify(servicoReserva, never()).agendarReuniao(any(), any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Dia sem horário livre pede outra data e não avança")
    void devePedirOutraDataSemHorarios() throws Exception {
        livres();

        ate("oi", "2", "1", dia.format(FORMATO_DATA));

        assertThat(ultimaResposta()).contains("Não há horários livres");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_DATA);
    }

    @Test
    @DisplayName("Lista de responsáveis vazia informa indisponibilidade e a dúvida continua respondendo")
    void deveInformarIndisponibilidadeSemResponsaveis() {
        empresa.setEmailsResponsaveisReuniao(List.of());

        ate("oi", "2");
        assertThat(ultimaResposta()).isEqualTo(AtendenteGambStrategy.MENSAGEM_REUNIAO_INDISPONIVEL);

        ate("1", "Como conectar o WhatsApp pelo QR Code?");
        assertThat(ultimaResposta()).contains("QR Code");
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Agenda desconectada informa indisponibilidade e não cria evento")
    void deveInformarIndisponibilidadeComAgendaDesconectada() throws Exception {
        when(servicoGoogleagenda.conectarAgenda()).thenThrow(new IllegalStateException("sem credencial"));

        ate("oi", "2");

        assertThat(ultimaResposta()).isEqualTo(AtendenteGambStrategy.MENSAGEM_REUNIAO_INDISPONIVEL);
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Falha ao consultar a agenda no meio do fluxo devolve ao menu sem criar evento")
    void deveVoltarAoMenuSeAAgendaFalhar() throws Exception {
        when(servicoGoogleagenda.horariosLivres(eq(AGENDA), any(LocalDate.class), anyList(), anyInt(), anyInt(), any()))
                .thenThrow(new java.io.IOException("fora do ar"));

        ate("oi", "2", "1", dia.format(FORMATO_DATA));

        assertThat(ultimaResposta()).isEqualTo(AtendenteGambStrategy.MENSAGEM_REUNIAO_INDISPONIVEL);
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_MENU);
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Recusar o resumo cancela o fluxo sem criar evento")
    void deveCancelarNaConfirmacao() {
        caminhoAteConfirmacao();

        enviar("2");

        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_MENU);
        verifyNoInteractions(servicoReserva);
    }

    @Test
    @DisplayName("Data passada é recusada sem consultar a agenda")
    void deveRecusarDataPassada() throws Exception {
        ate("oi", "2", "1", LocalDate.now().minusDays(1).format(FORMATO_DATA));

        assertThat(ultimaResposta()).contains("já passou");
        verify(servicoGoogleagenda, never()).horariosLivres(any(), any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Menu traz a opção de conhecer a Gamb")
    void menuTrazAOpcaoDeConhecerAGamb() {
        enviar("oi");

        assertThat(ultimaResposta()).contains("3").contains("Conhecer a Gamb");
    }

    @Test
    @DisplayName("Data escrita por extenso, com ponto ou com espaço segue para os horários livres")
    void deveAceitarDataEmVariosFormatos() {
        String comPonto = dia.getDayOfMonth() + "." + dia.getMonthValue() + "." + dia.getYear();
        String porExtenso = dia.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", java.util.Locale.of("pt", "BR")));
        String comEspaco = dia.getDayOfMonth() + " " + dia.getMonthValue() + " " + dia.getYear();

        for (String texto : List.of(comPonto, porExtenso, comEspaco, "pode ser dia " + comPonto + " mesmo?")) {
            gerenciadorSessao.setEstado(CLIENTE, EstadoUsuario.REUNIAO_DATA);
            enviar(texto);

            assertThat(gerenciadorSessao.getEstado(CLIENTE)).as(texto).isEqualTo(EstadoUsuario.REUNIAO_HORARIO);
            assertThat(ultimaResposta()).as(texto).contains("Horários livres em " + dia.format(FORMATO_DATA));
        }
    }

    @Test
    @DisplayName("Amanhã é entendido como a data de amanhã")
    void deveEntenderAmanha() {
        LocalDate amanha = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);

        ate("oi", "2", "1", "amanhã");

        assertThat(ultimaResposta()).contains("Horários livres em " + amanha.format(FORMATO_DATA));
    }

    @Test
    @DisplayName("Dia da semana deixa em dúvida: pergunta e o sim segue para os horários")
    void devePerguntarNaDuvidaESeguirAoConfirmar() {
        ate("oi", "2", "1", "tem sábado?");

        assertThat(ultimaResposta()).contains("Só para confirmar").contains("sábado");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_CONFIRMACAO_DATA);

        enviar("sim");

        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_HORARIO);
        assertThat(ultimaResposta()).contains("Horários livres");
    }

    @Test
    @DisplayName("Na dúvida, o cliente corrige com outra data e ela é usada")
    void deveAceitarCorrecaoDaData() {
        ate("oi", "2", "1", "dia 25", dia.format(FORMATO_DATA));

        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_HORARIO);
        assertThat(ultimaResposta()).contains("Horários livres em " + dia.format(FORMATO_DATA));
    }

    @Test
    @DisplayName("Dizer não na confirmação volta a pedir a data")
    void deveVoltarAPedirADataQuandoClienteNegar() {
        ate("oi", "2", "1", "tem sábado?", "não");

        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_DATA);
        assertThat(ultimaResposta()).contains("qual é a data");
    }

    @Test
    @DisplayName("Texto sem data repete o pedido sem avançar")
    void deveRepetirOPedidoQuandoNaoHouverData() {
        ate("oi", "2", "1", "não sei ainda");

        assertThat(ultimaResposta()).contains("Não entendi a data").contains("do seu jeito");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_DATA);
    }

    @Test
    @DisplayName("Opção 3 responde pela base comercial e convida a digitar 2 para marcar reunião")
    void deveResponderSobreAGambaPelaOpcaoTres() {
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn("A Gamb é uma startup que leva tecnologia a negócios.");

        ate("oi", "3");

        assertThat(ultimaResposta()).contains("startup").contains("Digite *2*").doesNotContain("wa.me");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_SOBRE);

        enviar("2");

        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.REUNIAO_MOTIVO);
    }

    @Test
    @DisplayName("Pergunta livre sobre a empresa, no menu ou na dúvida, vai para a resposta comercial")
    void deveDesviarPerguntaSobreAEmpresa() {
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn("Somos uma startup de tecnologia.");

        ate("oi", "o que vocês fazem?");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_SOBRE);
        assertThat(ultimaResposta()).contains("startup").contains("Digite *2*");

        ate("0", "1", "quem é a Gamb?");
        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_SOBRE);
        assertThat(ultimaResposta()).contains("startup");
    }

    @Test
    @DisplayName("Cliente descreve o problema: sem IA, a resposta liga ao que existe hoje, sem preço nem prazo")
    void deveOrientarQuandoClienteDescreveOProblema() {
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn(null);

        ate("oi", "3", "perco reservas porque demoro a responder, como vocês resolvem?");

        assertThat(ultimaResposta()).contains("assistente virtual").contains("Google Agenda").contains("sem prometer prazo")
                .doesNotContain("R$").doesNotContain("wa.me");
    }

    @Test
    @DisplayName("Resposta comercial da IA com valor em reais é trocada pelo texto sem preço")
    void naoDeveInformarValorNaRespostaComercial() {
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn("O plano custa R$ 199 por mês.");

        ate("oi", "3", "quanto custa?");

        assertThat(ultimaResposta()).doesNotContain("R$").contains("combinado");
    }

    @Test
    @DisplayName("Pergunta do app na dúvida continua no Suporte")
    void perguntaDoAppContinuaNoSuporte() {
        ate("oi", "1", "Como conectar o WhatsApp pelo QR Code?");

        assertThat(gerenciadorSessao.getEstado(CLIENTE)).isEqualTo(EstadoUsuario.ATENDENTE_DUVIDA);
        assertThat(ultimaResposta()).contains("QR Code").doesNotContain("Digite *2*");
    }
}
