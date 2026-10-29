package com.example.demo.strategy;

import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.model.ModuloEmpresa;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.example.demo.evento.EventoReservaConfirmada;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocacaoStrategyTest {

    private static final String REMETENTE = "5511988887777@c.us";
    private static final String CALENDAR_ID = "agenda-cliente@group.calendar.google.com";
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Mock private ServicoMensagem servicoMensagem;
    @Spy private ServicoMenu servicoMenu = new ServicoMenu();
    @Mock private ServicoGoogleagenda servicoGoogleagenda;
    @Mock private ServicoFuncionamento servicoFuncionamento;
    @Spy private GerenciadorSessao gerenciadorSessao = new GerenciadorSessao();
    @Mock private ApplicationEventPublisher publicadorEventos;
    @InjectMocks private LocacaoStrategy locacaoStrategy;

    @Test
    @DisplayName("INICIO monta o menu pelos módulos ativos ordenados")
    void deveEnviarMenuDinamicoNoInicio() {
        Empresa empresa = empresaComMenu();

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "oi", "INICIO");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("1️⃣ - Ver fotos do espaço"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("2️⃣ - Ver localização"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("3️⃣ - Pesquisar data disponível"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("4️⃣ - Solicitar uma reserva"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite o número da opção desejada"));
        verify(servicoMensagem, never()).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("DD/MM/AAAA"));
        verify(servicoMensagem, never()).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Falar com atendente virtual"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("Número do menu resolve o codigo_acao (fotos)")
    void deveRoteamentoDinamicoParaFotos() {
        Empresa empresa = empresaComMenu();
        empresa.setLinkFotoPrincipal("https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg");
        empresa.setLinkGaleria("https://drive.google.com/drive/folders/exemplo");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "1", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarImagemWPP(
                eq(empresa),
                eq(REMETENTE),
                eq("https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg"),
                eq(null));
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("https://drive.google.com/drive/folders/exemplo"));
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Deseja receber mais algumas fotos rápidas"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite *0*"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.ESPERANDO_RESPOSTA_GALERIA);
    }

    @Test
    @DisplayName("SIM na galeria dispara envio assíncrono e devolve o menu")
    void deveEnviarGaleriaQuandoClienteConfirmar() {
        Empresa empresa = empresaComMenu();
        empresa.setUrlsGaleria(List.of("https://res.cloudinary.com/demo/image/upload/v1/foto1.jpg"));
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_RESPOSTA_GALERIA);
        doAnswer(invocacao -> {
            Runnable aoTerminar = invocacao.getArgument(3);
            aoTerminar.run();
            return null;
        }).when(servicoMensagem).enviarGaleriaComIntervalo(any(), any(), any(), any());

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "ESPERANDO_RESPOSTA_GALERIA");

        verify(servicoMensagem).enviarGaleriaComIntervalo(eq(empresa), eq(REMETENTE), any(), any());
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Digite o número da opção desejada"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("NÃO na galeria devolve o menu sem enviar as fotos extras")
    void deveVoltarAoMenuQuandoRecusarGaleria() {
        Empresa empresa = empresaComMenu();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_RESPOSTA_GALERIA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "não", "ESPERANDO_RESPOSTA_GALERIA");

        verify(servicoMensagem, never()).enviarGaleriaComIntervalo(any(), any(), any(), any());
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Digite o número da opção desejada"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("SOLICITAR_RESERVA com flag desligada encaminha para atendente")
    void deveEncaminharParaAtendenteQuandoReservaAutomaticaDesligada() {
        Empresa empresa = empresaComMenu();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "4", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Logo um atendente vai te responder"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("SOLICITAR_RESERVA com flag ligada pede a data da reserva")
    void devePedirDataQuandoReservaAutomaticaLigada() {
        Empresa empresa = empresaComMenu();
        empresa.setPermiteReservaAutomatica(true);
        empresa.setGoogleCalendarId(CALENDAR_ID);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "4", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("Por favor, me diga qual data você deseja reservar."));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Diária: após a data pula o horário e pede o nome")
    void devePularHorarioQuandoForDiaria() {
        Empresa empresa = empresaComMenu();
        empresa.setLocacaoPorHora(false);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        String amanha = LocalDate.now(FUSO_BRASILIA).plusDays(1).format(FORMATO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, amanha, "RESERVA_ESPERANDO_DATA");

        assertThat(gerenciadorSessao.getReserva(REMETENTE).getHorario()).isEqualTo(DadosReservaSessao.HORARIO_DIARIA);
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("nome do responsável"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_NOME);
    }

    @Test
    @DisplayName("Antes da confirmação envia as regras e o resumo")
    void deveEnviarRegrasAntesDaConfirmacao() {
        Empresa empresa = empresaComMenu();
        empresa.setRegrasLocacao("Cancelar com 5 dias de antecedência");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_NOME);
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(REMETENTE);
        reserva.setData(LocalDate.of(2026, 9, 16));
        reserva.setHorario(DadosReservaSessao.HORARIO_DIARIA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "Maria", "RESERVA_ESPERANDO_NOME");

        InOrder ordem = inOrder(servicoMensagem);
        ordem.verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Cancelar com 5 dias de antecedência"));
        ordem.verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite SIM para confirmar"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_CONFIRMACAO);
        assertThat(gerenciadorSessao.getReserva(REMETENTE).getNomeCliente()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("GOOGLE_CALENDAR lista as próximas 15 datas")
    void deveListarProximas15DatasNoModuloAgenda() throws Exception {
        Empresa empresa = empresaComMenu();
        empresa.setGoogleCalendarId(CALENDAR_ID);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        when(servicoGoogleagenda.datasOcupadas(eq(CALENDAR_ID), eq(hoje), eq(hoje.plusDays(15))))
                .thenReturn(Set.of(hoje.plusDays(2)));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "5", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Agenda dos próximos 15 dias"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("✅ Livre"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("❌ Alugado"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("PESQUISAR_DATA pede a data e persiste ESPERANDO_DATA")
    void devePedirDataNaPesquisa() {
        Empresa empresa = empresaComMenu();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "3", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("qual data você deseja verificar"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("DD/MM/AAAA"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite *0*"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.ESPERANDO_DATA);
    }

    @Test
    @DisplayName("0, menu e voltar forçam INICIO e devolvem o menu principal")
    void deveVoltarAoMenuPrincipalComAtalho() {
        Empresa empresa = empresaComMenu();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        gerenciadorSessao.getOuCriarReserva(REMETENTE).setHorario("14:00");

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "voltar", "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite o número da opção desejada"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
        assertThat(gerenciadorSessao.getReserva(REMETENTE)).isNull();

        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        locacaoStrategy.processarMensagem(empresa, REMETENTE, "MENU", "ESPERANDO_DATA");
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);

        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_CONFIRMACAO);
        locacaoStrategy.processarMensagem(empresa, REMETENTE, "0", "RESERVA_CONFIRMACAO");
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("Opção inválida avisa o usuário sem mudar o estado")
    void naoDeveMudarEstadoQuandoOpcaoInvalida() {
        Empresa empresa = empresaComMenu();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "9", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Opção inválida"));
        verify(servicoMensagem, never()).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite *0*"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("A conversa não sofre amnésia: oi → pesquisar data → consulta a agenda")
    void deveManterContextoAteReceberAData() throws Exception {
        Empresa empresa = empresaComMenu();
        empresa.setGoogleCalendarId(CALENDAR_ID);
        LocalDate dataFutura = LocalDate.now(FUSO_BRASILIA).plusDays(5);
        when(servicoGoogleagenda.dataEstaLivre(eq(CALENDAR_ID), eq(dataFutura))).thenReturn(true);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "oi", "INICIO");
        locacaoStrategy.processarMensagem(empresa, REMETENTE, "3", "MENU_PRINCIPAL");
        locacaoStrategy.processarMensagem(empresa, REMETENTE, dataFutura.format(FORMATO_DATA), "ESPERANDO_DATA");

        InOrder ordem = inOrder(servicoMensagem);
        ordem.verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Pesquisar data disponível"));
        ordem.verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("qual data você deseja verificar"));
        ordem.verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite *0*"));
        ordem.verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("✅ Esta data está livre para locação!"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Data ocupada devolve a mensagem de reservada e reseta o estado")
    void deveInformarQuandoDataEstaReservada() throws Exception {
        Empresa empresa = empresaComAgenda();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        LocalDate dataFutura = LocalDate.now(FUSO_BRASILIA).plusDays(5);
        when(servicoGoogleagenda.dataEstaLivre(eq(CALENDAR_ID), eq(dataFutura))).thenReturn(false);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, dataFutura.format(FORMATO_DATA), "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("❌ Infelizmente esta data já está reservada."));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Agenda ausente avisa o usuário e reseta o estado")
    void deveAvisarQuandoCalendarioNaoFoiConfigurado() {
        Empresa empresa = empresa();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "16-09-2026", "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("calendário ainda não foi configurado"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Texto inválido pede o formato e mantém ESPERANDO_DATA")
    void devePedirFormatoQuandoDataForInvalida() {
        Empresa empresa = empresaComAgenda();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "amanhã", "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Não entendi essa data"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Pesquisa: data anterior a hoje não consulta a agenda e mantém ESPERANDO_DATA")
    void deveRecusarDataPassadaNaPesquisa() {
        Empresa empresa = empresaComAgenda();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        String ontem = LocalDate.now(FUSO_BRASILIA).minusDays(1).format(FORMATO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, ontem, "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa),
                eq(REMETENTE),
                eq("Essa data já passou. Envie uma data de hoje em diante, no formato DD-MM-AAAA, por exemplo 16-09-2026."));
        verifyNoInteractions(servicoGoogleagenda);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Pesquisa: data de hoje continua consultando a agenda")
    void deveConsultarAgendaQuandoDataDaPesquisaForHoje() throws Exception {
        Empresa empresa = empresaComAgenda();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
        when(servicoGoogleagenda.dataEstaLivre(eq(CALENDAR_ID), eq(hoje))).thenReturn(true);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, hoje.format(FORMATO_DATA), "ESPERANDO_DATA");

        verify(servicoGoogleagenda).dataEstaLivre(eq(CALENDAR_ID), eq(hoje));
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("✅ Esta data está livre para locação!"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Reserva: data anterior a hoje não consulta a agenda e mantém RESERVA_ESPERANDO_DATA")
    void deveRecusarDataPassadaNaReserva() {
        Empresa empresa = empresaComAgenda();
        empresa.setLocacaoPorHora(true);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        String ontem = LocalDate.now(FUSO_BRASILIA).minusDays(1).format(FORMATO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, ontem, "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa),
                eq(REMETENTE),
                eq("Essa data já passou. Envie uma data de hoje em diante, no formato DD-MM-AAAA, por exemplo 16-09-2026."));
        verifyNoInteractions(servicoGoogleagenda);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Horário: reserva de hoje com horário já passado não grava e mantém RESERVA_ESPERANDO_HORARIO")
    void deveRecusarHorarioPassadoQuandoReservaForHoje() {
        Empresa empresa = empresaComAgenda();
        empresa.setLocacaoPorHora(true);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(REMETENTE);
        reserva.setData(LocalDate.now(FUSO_BRASILIA));
        String horarioPassado = LocalTime.now(FUSO_BRASILIA).minusMinutes(1).format(FORMATO_HORA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, horarioPassado, "RESERVA_ESPERANDO_HORARIO");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa),
                eq(REMETENTE),
                eq("Esse horário de hoje já passou. Envie um horário que ainda não passou, no formato HH:MM, por exemplo 18:00."));
        verifyNoInteractions(servicoGoogleagenda);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        assertThat(reserva.getHorario()).isNull();
    }

    private void prepararConfirmacao(Empresa empresa) {
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(REMETENTE);
        reserva.setData(LocalDate.of(2026, 12, 25));
        reserva.setHorario(DadosReservaSessao.HORARIO_DIARIA);
        reserva.setNomeCliente("Maria Silva");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_CONFIRMACAO);
    }

    @Test
    @DisplayName("SIM na confirmação publica o evento, entra em RESERVA_REGISTRANDO e responde 'Anotei'")
    void devePublicarEventoEEntrarEmRegistrandoNoSim() {
        Empresa empresa = empresaComAgenda();
        prepararConfirmacao(empresa);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "RESERVA_CONFIRMACAO");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicadorEventos).publishEvent(captor.capture());
        EventoReservaConfirmada evento = (EventoReservaConfirmada) captor.getValue();
        assertThat(evento.getDataReserva()).isEqualTo("25-12-2026");
        assertThat(evento.getHorario()).isEqualTo(DadosReservaSessao.HORARIO_DIARIA);
        assertThat(evento.getNomeCliente()).isEqualTo("Maria Silva");
        assertThat(evento.getNumeroWhatsapp()).isEqualTo(REMETENTE);
        assertThat(evento.getEmpresa()).isSameAs(empresa);
        assertThat(java.util.UUID.fromString(evento.getIdPedido())).isNotNull();
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_REGISTRANDO);
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE),
                eq("Anotei seu pedido para 25-12-2026. Estou registrando na agenda e já confirmo."));
    }

    @Test
    @DisplayName("Estado muda para RESERVA_REGISTRANDO antes de publicar o evento")
    void deveMudarEstadoAntesDePublicar() {
        Empresa empresa = empresaComAgenda();
        prepararConfirmacao(empresa);
        doAnswer(invocacao -> {
            assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_REGISTRANDO);
            return null;
        }).when(publicadorEventos).publishEvent(any(Object.class));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "RESERVA_CONFIRMACAO");

        verify(publicadorEventos).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Segundo SIM em RESERVA_REGISTRANDO não publica de novo")
    void naoDevePublicarDeNovoNoSegundoSim() {
        Empresa empresa = empresaComAgenda();
        prepararConfirmacao(empresa);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "RESERVA_CONFIRMACAO");
        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "RESERVA_REGISTRANDO");

        verify(publicadorEventos, org.mockito.Mockito.times(1)).publishEvent(any(Object.class));
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("Ainda estou registrando sua reserva, já te respondo."));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_REGISTRANDO);
    }

    @Test
    @DisplayName("0, menu e voltar em RESERVA_REGISTRANDO não saem do estado")
    void naoDeveSairDoRegistroComComandosDeMenu() {
        Empresa empresa = empresaComMenu();
        for (String comando : List.of("0", "menu", "voltar", "MENU", "qualquer coisa")) {
            gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_REGISTRANDO);

            locacaoStrategy.processarMensagem(empresa, REMETENTE, comando, "RESERVA_REGISTRANDO");

            assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_REGISTRANDO);
        }

        verify(servicoMensagem, org.mockito.Mockito.times(5)).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("Ainda estou registrando sua reserva, já te respondo."));
        verify(servicoMensagem, never()).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Digite o número da opção desejada"));
        verify(publicadorEventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Reserva automática ligada e sem agenda responde como reserva manual desde o início")
    void deveAtenderComoManualQuandoNaoHaAgenda() {
        Empresa empresa = empresaComMenu();
        empresa.setPermiteReservaAutomatica(true);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "4", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("Recebemos sua mensagem. Logo um atendente vai te responder."));
        verify(servicoMensagem, never()).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("qual data você deseja reservar"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("Reserva automática ligada com agenda em branco também vira reserva manual")
    void deveAtenderComoManualQuandoAgendaEmBranco() {
        Empresa empresa = empresaComMenu();
        empresa.setPermiteReservaAutomatica(true);
        empresa.setGoogleCalendarId("   ");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "4", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Logo um atendente vai te responder"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    private static Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setNome("Recanto Teste");
        empresa.setMensagemSaudacao("Olá!");
        empresa.setRamoDeAtuacao("LOCACAO");
        return empresa;
    }

    private static Empresa empresaComAgenda() {
        Empresa empresa = empresa();
        empresa.setGoogleCalendarId(CALENDAR_ID);
        return empresa;
    }

    private static Empresa empresaComMenu() {
        Empresa empresa = empresa();
        adicionarModulo(empresa, "VER_FOTOS", "Ver fotos do espaço", 1, true);
        adicionarModulo(empresa, "VER_LOCALIZACAO", "Ver localização", 2, true);
        adicionarModulo(empresa, "PESQUISAR_DATA", "Pesquisar data disponível", 3, true);
        adicionarModulo(empresa, "SOLICITAR_RESERVA", "Solicitar uma reserva", 4, true);
        adicionarModulo(empresa, "GOOGLE_CALENDAR", "Consultar agenda", 5, true);
        adicionarModulo(empresa, "IA_GEMINI", "Falar com atendente virtual", 6, false);
        return empresa;
    }

    private static void adicionarModulo(Empresa empresa, String codigo, String texto, int ordem, boolean ativo) {
        ModuloEmpresa modulo = new ModuloEmpresa();
        modulo.setCodigoAcao(codigo);
        modulo.setTextoMenu(texto);
        modulo.setOrdemExibicao(ordem);
        modulo.setAtivo(ativo);
        empresa.getModulosAtivos().add(modulo);
    }

    private static final ServicoFuncionamento.Janela JANELA_ABERTA =
            new ServicoFuncionamento.Janela(true, LocalTime.of(8, 0), LocalTime.of(22, 0), 60, null);

    private Empresa empresaPorHora() {
        Empresa empresa = empresaComAgenda();
        empresa.setLocacaoPorHora(true);
        empresa.setPermiteReservaAutomatica(true);
        return empresa;
    }

    private LocalDate daquiADias(int dias) {
        return LocalDate.now(FUSO_BRASILIA).plusDays(dias);
    }

    private DadosReservaSessao pedidoComDatas(String horario, LocalDate... datas) {
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(REMETENTE);
        reserva.setHorario(horario);
        for (LocalDate data : datas) {
            reserva.adicionarData(data);
        }
        return reserva;
    }

    @Test
    @DisplayName("Por hora com a opção ligada pergunta a data em vez de virar reserva manual")
    void devePerguntarADataQuandoLocacaoPorHoraComReservaAutomatica() {
        Empresa empresa = empresaComMenu();
        empresa.setGoogleCalendarId(CALENDAR_ID);
        empresa.setLocacaoPorHora(true);
        empresa.setPermiteReservaAutomatica(true);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "4", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("qual data você deseja reservar"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Por hora: após a data lista os horários livres e pede o horário")
    void deveListarHorariosLivresAposAData() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horariosLivres(eq(CALENDAR_ID), eq(data), any(), eq(60), eq(0), isNull()))
                .thenReturn(List.of(LocalTime.of(14, 0), LocalTime.of(15, 0)));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, data.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("14:00, 15:00"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Qual horário você deseja"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        assertThat(gerenciadorSessao.getReserva(REMETENTE).getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("Por hora: data de hoje filtra os horários que já passaram")
    void deveFiltrarHorariosPassadosQuandoADataForHoje() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate hoje = daquiADias(0);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(hoje))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horariosLivres(eq(CALENDAR_ID), eq(hoje), any(), eq(60), eq(0), notNull()))
                .thenReturn(List.of(LocalTime.of(23, 0)));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, hoje.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoGoogleagenda, never()).dataEstaLivre(any(), any());
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
    }

    @Test
    @DisplayName("Por hora: data fechada diz o motivo e sugere as próximas datas abertas")
    void deveInformarDataFechadaComMotivoEProximasDatas() {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data)))
                .thenReturn(ServicoFuncionamento.Janela.fechada("feriado: Natal"));
        when(servicoFuncionamento.proximasDatasAbertas(eq(empresa), eq(data.plusDays(1)), eq(3)))
                .thenReturn(List.of(data.plusDays(2)));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, data.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Não abrimos em " + data.format(FORMATO_DATA)));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("feriado: Natal"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains(data.plusDays(2).format(FORMATO_DATA)));
        verifyNoInteractions(servicoGoogleagenda);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Por hora: dia sem horário livre pede outra data")
    void devePedirOutraDataQuandoNaoHaHorarioLivre() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horariosLivres(any(), eq(data), any(), anyInt(), anyInt(), any())).thenReturn(List.of());

        locacaoStrategy.processarMensagem(empresa, REMETENTE, data.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Não há horários livres"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Por hora: data além de 180 dias é recusada")
    void deveRecusarDataAlemDoLimite() {
        Empresa empresa = empresaPorHora();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);

        locacaoStrategy.processarMensagem(
                empresa, REMETENTE, daquiADias(181).format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("até 180 dias"));
        verifyNoInteractions(servicoFuncionamento);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Horário livre vai para a pergunta de mais datas")
    void deveAceitarHorarioLivreEPerguntarMaisDatas() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(REMETENTE);
        reserva.setData(data);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horarioEstaLivre(
                eq(CALENDAR_ID), eq(data), eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)), eq(0))).thenReturn(true);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "14:00", "RESERVA_ESPERANDO_HORARIO");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("mais alguma data"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_MAIS_DATAS);
        assertThat(reserva.getHorario()).isEqualTo("14:00");
        assertThat(reserva.getDatas()).containsExactly(data);
    }

    @Test
    @DisplayName("Horário fora do passo da duração mostra os horários de início")
    void deveRecusarHorarioForaDoPasso() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        gerenciadorSessao.getOuCriarReserva(REMETENTE).setData(data);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "14:30", "RESERVA_ESPERANDO_HORARIO");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Horários de início"));
        verify(servicoGoogleagenda, never()).horarioEstaLivre(any(), any(), any(), any(), anyInt());
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
    }

    @Test
    @DisplayName("Horário ocupado na agenda é recusado e mantém RESERVA_ESPERANDO_HORARIO")
    void deveRecusarHorarioOcupado() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        gerenciadorSessao.getOuCriarReserva(REMETENTE).setData(data);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horarioEstaLivre(any(), eq(data), any(), any(), anyInt())).thenReturn(false);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "14:00", "RESERVA_ESPERANDO_HORARIO");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("já está reservado"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
        assertThat(gerenciadorSessao.getReserva(REMETENTE).getDatas()).isEmpty();
    }

    @Test
    @DisplayName("Mais datas: SIM pede a próxima data, a segunda data entra e NÃO segue para o nome")
    void deveReservarVariasDatasComOMesmoHorario() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate primeira = daquiADias(10);
        LocalDate segunda = daquiADias(17);
        DadosReservaSessao reserva = pedidoComDatas("14:00", primeira);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_MAIS_DATAS);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(segunda))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horarioEstaLivre(
                eq(CALENDAR_ID), eq(segunda), eq(LocalTime.of(14, 0)), eq(LocalTime.of(15, 0)), eq(0))).thenReturn(true);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "RESERVA_MAIS_DATAS");
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, segunda.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_MAIS_DATAS);
        assertThat(reserva.getDatas()).containsExactly(primeira, segunda);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "não", "RESERVA_MAIS_DATAS");
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("nome do responsável"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_NOME);
    }

    @Test
    @DisplayName("Mais datas: data que não comporta o horário escolhido cai e não entra no pedido")
    void deveRecusarDataQueNaoComportaOHorarioEscolhido() {
        Empresa empresa = empresaPorHora();
        LocalDate primeira = daquiADias(10);
        LocalDate sabado = daquiADias(11);
        DadosReservaSessao reserva = pedidoComDatas("14:00", primeira);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(sabado))).thenReturn(
                new ServicoFuncionamento.Janela(true, LocalTime.of(16, 0), LocalTime.of(22, 0), 60, null));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, sabado.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Esse horário não está disponível"));
        assertThat(reserva.getDatas()).containsExactly(primeira);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    @Test
    @DisplayName("Mais datas: ao chegar em 10 datas segue direto para o nome")
    void deveParaNoLimiteDeDezDatas() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate[] existentes = new LocalDate[9];
        for (int i = 0; i < 9; i++) {
            existentes[i] = daquiADias(10 + i);
        }
        pedidoComDatas("14:00", existentes);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);
        LocalDate decima = daquiADias(30);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(decima))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.horarioEstaLivre(any(), eq(decima), any(), any(), anyInt())).thenReturn(true);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, decima.format(FORMATO_DATA), "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("limite de 10 datas"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_NOME);
    }

    @Test
    @DisplayName("Mais datas: TIRAR remove a data do pedido")
    void deveTirarUmaDataDoPedido() {
        Empresa empresa = empresaPorHora();
        LocalDate primeira = daquiADias(10);
        LocalDate segunda = daquiADias(17);
        DadosReservaSessao reserva = pedidoComDatas("14:00", primeira, segunda);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_MAIS_DATAS);

        locacaoStrategy.processarMensagem(
                empresa, REMETENTE, "tirar " + segunda.format(FORMATO_DATA), "RESERVA_MAIS_DATAS");

        assertThat(reserva.getDatas()).containsExactly(primeira);
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_MAIS_DATAS);
    }

    @Test
    @DisplayName("Por hora: agenda dos próximos dias mostra Lotado e Fechado em vez de Alugado")
    void deveMostrarLotadoEFechadoNaAgendaPorHora() throws Exception {
        Empresa empresa = empresaComMenu();
        empresa.setGoogleCalendarId(CALENDAR_ID);
        empresa.setLocacaoPorHora(true);
        LocalDate hoje = daquiADias(0);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);
        when(servicoGoogleagenda.ocupacoesDoPeriodo(eq(CALENDAR_ID), eq(hoje), eq(hoje.plusDays(15))))
                .thenReturn(List.of());
        when(servicoFuncionamento.janelaDa(eq(empresa), any())).thenReturn(JANELA_ABERTA);
        when(servicoFuncionamento.situacaoDoDia(eq(empresa), any(), any(), any()))
                .thenReturn(ServicoFuncionamento.Situacao.LIVRE);
        when(servicoFuncionamento.situacaoDoDia(eq(empresa), eq(hoje), any(), any()))
                .thenReturn(ServicoFuncionamento.Situacao.LOTADO);
        when(servicoFuncionamento.situacaoDoDia(eq(empresa), eq(hoje.plusDays(1)), any(), any()))
                .thenReturn(ServicoFuncionamento.Situacao.FECHADO);
        when(servicoFuncionamento.situacaoDoDia(eq(empresa), eq(hoje.plusDays(2)), any(), any()))
                .thenReturn(ServicoFuncionamento.Situacao.LIVRE);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "5", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("❌ Lotado"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("🚫 Fechado"));
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("✅ Livre"));
        verify(servicoGoogleagenda, never()).datasOcupadas(any(), any(), any());
    }

    @Test
    @DisplayName("Por hora: pesquisar data lista os horários livres, não o dia inteiro como reservado")
    void devePesquisarDataPorHoraListandoHorariosLivres() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.ocupacoesDoDia(CALENDAR_ID, data)).thenReturn(List.of());
        when(servicoFuncionamento.iniciosLivres(eq(empresa), eq(data), any(), any()))
                .thenReturn(List.of(LocalTime.of(14, 0), LocalTime.of(15, 0)));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, data.format(FORMATO_DATA), "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("14:00, 15:00"));
        verify(servicoGoogleagenda, never()).dataEstaLivre(any(), any());
    }

    @Test
    @DisplayName("Por hora: pesquisar data sem horário livre avisa que está tudo reservado")
    void devePesquisarDataPorHoraLotada() throws Exception {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data))).thenReturn(JANELA_ABERTA);
        when(servicoGoogleagenda.ocupacoesDoDia(CALENDAR_ID, data)).thenReturn(List.of());
        when(servicoFuncionamento.iniciosLivres(eq(empresa), eq(data), any(), any())).thenReturn(List.of());

        locacaoStrategy.processarMensagem(empresa, REMETENTE, data.format(FORMATO_DATA), "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Todos os horários desta data já estão reservados"));
    }

    @Test
    @DisplayName("Por hora: pesquisar data fechada informa o motivo")
    void devePesquisarDataPorHoraFechada() {
        Empresa empresa = empresaPorHora();
        LocalDate data = daquiADias(10);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        when(servicoFuncionamento.janelaDa(eq(empresa), eq(data)))
                .thenReturn(ServicoFuncionamento.Janela.fechada("feriado: Natal"));

        locacaoStrategy.processarMensagem(empresa, REMETENTE, data.format(FORMATO_DATA), "ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("feriado: Natal"));
    }

    @Test
    @DisplayName("SIM na confirmação por hora publica um só evento com todas as datas")
    void devePublicarEventoComTodasAsDatasPorHora() {
        Empresa empresa = empresaPorHora();
        LocalDate primeira = daquiADias(10);
        LocalDate segunda = daquiADias(17);
        DadosReservaSessao reserva = pedidoComDatas("14:00", primeira, segunda);
        reserva.setNomeCliente("Maria Silva");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_CONFIRMACAO);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "sim", "RESERVA_CONFIRMACAO");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicadorEventos).publishEvent(captor.capture());
        EventoReservaConfirmada evento = (EventoReservaConfirmada) captor.getValue();
        assertThat(evento.isPorHora()).isTrue();
        assertThat(evento.getDatas()).containsExactly(primeira.format(FORMATO_DATA), segunda.format(FORMATO_DATA));
        assertThat(evento.getHorario()).isEqualTo("14:00");
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_REGISTRANDO);
    }
}
