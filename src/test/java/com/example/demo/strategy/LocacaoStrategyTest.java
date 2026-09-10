package com.example.demo.strategy;

import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.model.ModuloEmpresa;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocacaoStrategyTest {

    private static final String REMETENTE = "5511988887777@c.us";
    private static final String CALENDAR_ID = "agenda-cliente@group.calendar.google.com";

    @Mock private ServicoMensagem servicoMensagem;
    @Spy private ServicoMenu servicoMenu = new ServicoMenu();
    @Mock private ServicoGoogleagenda servicoGoogleagenda;
    @Spy private GerenciadorSessao gerenciadorSessao = new GerenciadorSessao();
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
        verify(servicoMensagem, never()).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Falar com atendente virtual"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("Número do menu resolve o codigo_acao (fotos)")
    void deveRoteamentoDinamicoParaFotos() {
        Empresa empresa = empresaComMenu();
        empresa.setLinkFotoPrincipal("https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg");
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.MENU_PRINCIPAL);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "1", "MENU_PRINCIPAL");

        verify(servicoMensagem).enviarImagemWPP(
                eq(empresa),
                eq(REMETENTE),
                eq("https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg"),
                eq(null));
        verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), contains("Deseja ver mais fotos do ambiente"));
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

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "16-09-2026", "RESERVA_ESPERANDO_DATA");

        assertThat(gerenciadorSessao.getReserva(REMETENTE).getHorario()).isEqualTo(DadosReservaSessao.HORARIO_DIARIA);
        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("nome do responsável"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_NOME);
    }

    @Test
    @DisplayName("Por hora: após a data pede o horário")
    void devePedirHorarioQuandoForLocacaoPorHora() {
        Empresa empresa = empresaComMenu();
        empresa.setLocacaoPorHora(true);
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.RESERVA_ESPERANDO_DATA);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "16-09-2026", "RESERVA_ESPERANDO_DATA");

        verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Qual horário você deseja"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
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
                eq(empresa), eq(REMETENTE), eq("Por favor, me diga qual data você deseja verificar."));
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
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.MENU_PRINCIPAL);
    }

    @Test
    @DisplayName("A conversa não sofre amnésia: oi → pesquisar data → consulta a agenda")
    void deveManterContextoAteReceberAData() throws Exception {
        Empresa empresa = empresaComMenu();
        empresa.setGoogleCalendarId(CALENDAR_ID);
        when(servicoGoogleagenda.dataEstaLivre(eq(CALENDAR_ID), eq(LocalDate.of(2026, 9, 16)))).thenReturn(true);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "oi", "INICIO");
        locacaoStrategy.processarMensagem(empresa, REMETENTE, "3", "MENU_PRINCIPAL");
        locacaoStrategy.processarMensagem(empresa, REMETENTE, "16-09-2026", "ESPERANDO_DATA");

        InOrder ordem = inOrder(servicoMensagem);
        ordem.verify(servicoMensagem).enviarMensagemWPP(eq(empresa), eq(REMETENTE), contains("Pesquisar data disponível"));
        ordem.verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("Por favor, me diga qual data você deseja verificar."));
        ordem.verify(servicoMensagem).enviarMensagemWPP(
                eq(empresa), eq(REMETENTE), eq("✅ Esta data está livre para locação!"));
        assertThat(gerenciadorSessao.getEstado(REMETENTE)).isEqualTo(EstadoUsuario.INICIO);
    }

    @Test
    @DisplayName("Data ocupada devolve a mensagem de reservada e reseta o estado")
    void deveInformarQuandoDataEstaReservada() throws Exception {
        Empresa empresa = empresaComAgenda();
        gerenciadorSessao.setEstado(REMETENTE, EstadoUsuario.ESPERANDO_DATA);
        when(servicoGoogleagenda.dataEstaLivre(eq(CALENDAR_ID), eq(LocalDate.of(2026, 9, 16)))).thenReturn(false);

        locacaoStrategy.processarMensagem(empresa, REMETENTE, "16-09-2026", "ESPERANDO_DATA");

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
}
