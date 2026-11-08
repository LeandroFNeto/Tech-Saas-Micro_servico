package com.example.demo.strategy;

import com.example.demo.model.DadosReuniaoSessao;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.model.MotivoReuniao;
import com.example.demo.servico.ConversaCurta;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.InterpretadorDeData;
import com.example.demo.servico.LimiteDiarioMensagens;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import com.example.demo.util.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AtendenteGambStrategy implements ModuloAtendimentoStrategy {

    public static final String RAMO = "ATENDENTE_GAMB";

    private static final Logger log = LoggerFactory.getLogger(AtendenteGambStrategy.class);
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern PADRAO_HORA = Pattern.compile("^(\\d{1,2})(?:[:h](\\d{2})?)$");
    private static final int MAXIMO_OPCOES = 8;

    static final String MENSAGEM_MENU =
            "Olá! Eu sou a atendente da Gamb. Como posso ajudar?\n\n"
                    + "1️⃣ - Tirar uma dúvida sobre o app\n"
                    + "2️⃣ - Marcar reunião com a equipe\n"
                    + "3️⃣ - Conhecer a Gamb e ver como podemos ajudar no seu negócio\n\n"
                    + "Digite o número da opção desejada.";
    static final String MENSAGEM_LIMITE =
            "Você atingiu o limite de mensagens de hoje. Volte amanhã para continuarmos a conversa.";
    static final String MENSAGEM_DUVIDA =
            "Pode perguntar. Respondo com base nos tutoriais e avisos do app. Digite *0* para voltar ao menu.";
    static final String ORIENTACAO_PAINEL = "Para consultar os dados da sua empresa, use o painel do app.";
    static final String MENSAGEM_REUNIAO_INDISPONIVEL =
            "No momento o agendamento de reunião está indisponível. Você ainda pode tirar uma dúvida: digite *1*.";
    static final String MENSAGEM_MOTIVO =
            "Qual é o motivo da reunião?\n\n1️⃣ - Ajuste\n2️⃣ - Nova inovação\n\nDigite *0* para voltar ao menu.";
    static final String MENSAGEM_PEDIR_DATA =
            "Para qual data? Pode escrever do seu jeito, por exemplo 16/09/2026 ou 16 de setembro.";
    static final String CONVITE_REUNIAO =
            "\n\nDigite *2* para marcar uma reunião com a equipe ou *0* para voltar ao menu.";

    @Autowired private ServicoMensagem servicoMensagem;
    @Autowired private GerenciadorSessao gerenciadorSessao;
    @Autowired private AssistenteTutoriaService assistenteTutoriaService;
    @Autowired private ServicoGoogleagenda servicoGoogleagenda;
    @Autowired private ServicoFuncionamento servicoFuncionamento;
    @Autowired private ServicoReserva servicoReserva;
    @Autowired private LimiteDiarioMensagens limiteDiarioMensagens;
    private final InterpretadorDeData interpretadorDeData = new InterpretadorDeData();
    private final ConversaCurta conversaCurta = new ConversaCurta();

    private static final Set<String> COMANDOS_VOLTA_MENU = Set.of("menu", "voltar");
    private static final Set<String> COMANDOS_REPETEM_MENU = Set.of("sim", "nao", "sair", "cancelar", "ok");

    @Override
    public String getRamoDeAtuacao() {
        return RAMO;
    }

    private String sessao(Empresa empresa) {
        return empresa == null ? null : empresa.getSessaoWhatsapp();
    }

    @Override
    public void processarMensagem(Empresa empresa, String numeroCliente, String textoRecebido, String estadoAtual) {
        String texto = textoRecebido == null ? "" : textoRecebido.trim();
        log.info("Atendente Gamb recebeu mensagem de {} ({})", MascaraLog.numero(numeroCliente), MascaraLog.tamanho(texto));

        if (!limiteDiarioMensagens.registrarSePermitido(numeroCliente)) {
            enviar(empresa, numeroCliente, MENSAGEM_LIMITE);
            return;
        }

        if (texto.equals("0") || COMANDOS_VOLTA_MENU.contains(comando(texto))) {
            mostrarMenu(empresa, numeroCliente);
            return;
        }

        EstadoUsuario estado = gerenciadorSessao.getEstado(sessao(empresa), numeroCliente);
        if (tratouConversaCurta(empresa, numeroCliente, texto, estado)) {
            return;
        }

        switch (estado) {
            case ATENDENTE_MENU -> tratarMenu(empresa, numeroCliente, texto);
            case ATENDENTE_DUVIDA -> responderDuvida(empresa, numeroCliente, texto);
            case ATENDENTE_SOBRE -> tratarSobre(empresa, numeroCliente, texto);
            case REUNIAO_MOTIVO -> tratarMotivo(empresa, numeroCliente, texto);
            case REUNIAO_DATA -> tratarData(empresa, numeroCliente, texto);
            case REUNIAO_CONFIRMACAO_DATA -> tratarConfirmacaoData(empresa, numeroCliente, texto);
            case REUNIAO_HORARIO -> tratarHorario(empresa, numeroCliente, texto);
            case REUNIAO_CONFIRMACAO -> tratarConfirmacao(empresa, numeroCliente, texto);
            default -> mostrarMenu(empresa, numeroCliente);
        }
    }

    private boolean tratouConversaCurta(Empresa empresa, String numeroCliente, String texto, EstadoUsuario estado) {
        boolean noMenu = estado == EstadoUsuario.ATENDENTE_MENU || !emFluxoDaAtendente(estado);
        if (!noMenu && estado != EstadoUsuario.ATENDENTE_DUVIDA && estado != EstadoUsuario.ATENDENTE_SOBRE) {
            return false;
        }
        Optional<ConversaCurta.Resposta> curta = conversaCurta.responder(numeroCliente, texto);
        if (curta.isEmpty()) {
            return false;
        }
        ConversaCurta.Resposta resposta = curta.get();
        boolean abreMenu = noMenu && (resposta.tipo() == ConversaCurta.Tipo.CUMPRIMENTO
                || resposta.tipo() == ConversaCurta.Tipo.BEM_ESTAR);
        if (abreMenu) {
            String s = sessao(empresa);
            gerenciadorSessao.limparReuniao(s, numeroCliente);
            gerenciadorSessao.limparDataPendente(s, numeroCliente);
            gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.ATENDENTE_MENU);
            enviar(empresa, numeroCliente, resposta.texto() + "\n\n" + MENSAGEM_MENU);
            return true;
        }
        enviar(empresa, numeroCliente, resposta.texto());
        return true;
    }

    private boolean emFluxoDaAtendente(EstadoUsuario estado) {
        return switch (estado) {
            case ATENDENTE_MENU, ATENDENTE_DUVIDA, ATENDENTE_SOBRE, REUNIAO_MOTIVO, REUNIAO_DATA,
                    REUNIAO_CONFIRMACAO_DATA, REUNIAO_HORARIO, REUNIAO_CONFIRMACAO -> true;
            default -> false;
        };
    }

    private void mostrarMenu(Empresa empresa, String numeroCliente) {
        String s = sessao(empresa);
        gerenciadorSessao.limparReuniao(s, numeroCliente);
        gerenciadorSessao.limparDataPendente(s, numeroCliente);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.ATENDENTE_MENU);
        enviar(empresa, numeroCliente, MENSAGEM_MENU);
    }

    private void tratarMenu(Empresa empresa, String numeroCliente, String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.equals("1") || normalizado.contains("duvida")) {
            gerenciadorSessao.setEstado(sessao(empresa), numeroCliente, EstadoUsuario.ATENDENTE_DUVIDA);
            enviar(empresa, numeroCliente, MENSAGEM_DUVIDA);
            return;
        }
        if (normalizado.equals("2") || normalizado.contains("reuni")) {
            iniciarReuniao(empresa, numeroCliente);
            return;
        }
        if (normalizado.equals("3") || AssistenteTutoriaService.temIntencaoComercial(texto)) {
            responderSobre(empresa, numeroCliente, normalizado.equals("3") ? "Quem é a Gamb e como vocês podem ajudar o meu negócio?" : texto);
            return;
        }
        if (normalizado.matches("^[0-9]+$") || normalizado.length() < 3 || COMANDOS_REPETEM_MENU.contains(comando(texto))) {
            enviar(empresa, numeroCliente, MENSAGEM_MENU);
            return;
        }
        if (AssistenteTutoriaService.ehPerguntaDoApp(texto)) {
            responderDuvida(empresa, numeroCliente, texto);
            return;
        }
        responderSobre(empresa, numeroCliente, texto);
    }

    private void tratarSobre(Empresa empresa, String numeroCliente, String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.equals("2") || normalizado.contains("reuni")) {
            iniciarReuniao(empresa, numeroCliente);
            return;
        }
        responderSobre(empresa, numeroCliente, texto);
    }

    private void responderSobre(Empresa empresa, String numeroCliente, String texto) {
        gerenciadorSessao.setEstado(sessao(empresa), numeroCliente, EstadoUsuario.ATENDENTE_SOBRE);
        String resposta = assistenteTutoriaService.responderComercialWhatsapp(texto).resposta();
        enviar(empresa, numeroCliente, resposta + CONVITE_REUNIAO);
    }

    private void responderDuvida(Empresa empresa, String numeroCliente, String texto) {
        if (AssistenteTutoriaService.temIntencaoComercial(texto)) {
            responderSobre(empresa, numeroCliente, texto);
            return;
        }
        String resposta = assistenteTutoriaService.responderSuporte(texto).resposta();
        if (AssistenteTutoriaService.RECUSA_SUPORTE.equals(resposta)) {
            resposta = resposta + " " + ORIENTACAO_PAINEL;
        }
        enviar(empresa, numeroCliente, resposta + "\n\nDigite *0* para voltar ao menu.");
    }

    private void iniciarReuniao(Empresa empresa, String numeroCliente) {
        if (!reuniaoDisponivel(empresa)) {
            enviar(empresa, numeroCliente, MENSAGEM_REUNIAO_INDISPONIVEL);
            return;
        }
        String s = sessao(empresa);
        gerenciadorSessao.limparReuniao(s, numeroCliente);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_MOTIVO);
        enviar(empresa, numeroCliente, MENSAGEM_MOTIVO);
    }

    private boolean reuniaoDisponivel(Empresa empresa) {
        if (empresa.getEmailsResponsaveisReuniao().isEmpty()) {
            return false;
        }
        String agenda = empresa.getGoogleCalendarId();
        if (agenda == null || agenda.isBlank()) {
            return false;
        }
        try {
            servicoGoogleagenda.conectarAgenda();
            return true;
        } catch (IllegalStateException e) {
            log.warn("Google Agenda da Gamb desconectada para o agendamento de reunião");
            return false;
        }
    }

    private void tratarMotivo(Empresa empresa, String numeroCliente, String texto) {
        String normalizado = normalizar(texto);
        MotivoReuniao motivo = null;
        if (normalizado.equals("1") || normalizado.contains("ajuste")) {
            motivo = MotivoReuniao.AJUSTE;
        } else if (normalizado.equals("2") || normalizado.contains("inov")) {
            motivo = MotivoReuniao.NOVA_INOVACAO;
        }
        if (motivo == null) {
            enviar(empresa, numeroCliente, MENSAGEM_MOTIVO);
            return;
        }
        String s = sessao(empresa);
        gerenciadorSessao.getOuCriarReuniao(s, numeroCliente).setMotivo(motivo);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_DATA);
        enviar(empresa, numeroCliente, MENSAGEM_PEDIR_DATA);
    }

    private void tratarData(Empresa empresa, String numeroCliente, String texto) {
        InterpretadorDeData.Resultado resultado = interpretadorDeData.interpretar(texto, LocalDate.now(FUSO_BRASILIA));
        if (resultado instanceof InterpretadorDeData.Certa certa) {
            seguirComData(empresa, numeroCliente, certa.data());
            return;
        }
        if (resultado instanceof InterpretadorDeData.Duvidosa duvidosa && duvidosa.sugestao() != null) {
            perguntarSeEssaEhAData(empresa, numeroCliente, duvidosa.sugestao());
            return;
        }
        enviar(empresa, numeroCliente, "Não entendi a data. " + MENSAGEM_PEDIR_DATA);
    }

    private void perguntarSeEssaEhAData(Empresa empresa, String numeroCliente, LocalDate sugestao) {
        String s = sessao(empresa);
        gerenciadorSessao.guardarDataPendente(s, numeroCliente, sugestao, EstadoUsuario.REUNIAO_DATA);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_CONFIRMACAO_DATA);
        enviar(empresa, numeroCliente, "Só para confirmar: você quer dizer " + descreverData(sugestao)
                + "? Responda *sim* ou me diga a data certa.");
    }

    private String descreverData(LocalDate data) {
        String diaSemana = data.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.of("pt", "BR"));
        return diaSemana + ", " + data.format(FORMATO_DATA);
    }

    private void tratarConfirmacaoData(Empresa empresa, String numeroCliente, String texto) {
        String s = sessao(empresa);
        GerenciadorSessao.DataPendente pendente = gerenciadorSessao.obterDataPendente(s, numeroCliente);
        if (pendente == null) {
            gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_DATA);
            enviar(empresa, numeroCliente, MENSAGEM_PEDIR_DATA);
            return;
        }
        String resposta = normalizar(texto).replaceAll("[.! ]+$", "");
        if (resposta.matches("^(sim|s|isso|isso mesmo|ok|pode ser|pode|claro|exato|exatamente|correto|certo|confirmo|e|essa mesma)$")) {
            gerenciadorSessao.limparDataPendente(s, numeroCliente);
            gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_DATA);
            seguirComData(empresa, numeroCliente, pendente.data());
            return;
        }
        if (resposta.matches("^(nao|n|negativo|errado|nao e|nao e essa|outra)$")) {
            gerenciadorSessao.limparDataPendente(s, numeroCliente);
            gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_DATA);
            enviar(empresa, numeroCliente, "Tudo bem! Então qual é a data? " + MENSAGEM_PEDIR_DATA);
            return;
        }
        InterpretadorDeData.Resultado resultado = interpretadorDeData.interpretar(texto, LocalDate.now(FUSO_BRASILIA));
        if (resultado instanceof InterpretadorDeData.Certa certa) {
            gerenciadorSessao.limparDataPendente(s, numeroCliente);
            gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_DATA);
            seguirComData(empresa, numeroCliente, certa.data());
            return;
        }
        if (resultado instanceof InterpretadorDeData.Duvidosa duvidosa && duvidosa.sugestao() != null) {
            perguntarSeEssaEhAData(empresa, numeroCliente, duvidosa.sugestao());
            return;
        }
        enviar(empresa, numeroCliente, "Não entendi. Responda *sim* se a data é " + descreverData(pendente.data())
                + " ou me diga a data certa.");
    }

    private void seguirComData(Empresa empresa, String numeroCliente, LocalDate data) {
        LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
        if (data.isBefore(hoje)) {
            enviar(empresa, numeroCliente, "Essa data já passou. " + MENSAGEM_PEDIR_DATA);
            return;
        }
        if (data.isAfter(hoje.plusDays(ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE))) {
            enviar(empresa, numeroCliente, "Só marcamos reuniões até " + ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE
                    + " dias à frente. " + MENSAGEM_PEDIR_DATA);
            return;
        }
        if (!servicoFuncionamento.janelaDa(empresa, data).aberto()) {
            enviar(empresa, numeroCliente, "Não temos atendimento em " + data.format(FORMATO_DATA) + ". " + MENSAGEM_PEDIR_DATA);
            return;
        }

        List<LocalTime> livres;
        try {
            livres = horariosLivres(empresa, data);
        } catch (Exception e) {
            reuniaoFalhou(empresa, numeroCliente, e);
            return;
        }
        if (livres.isEmpty()) {
            enviar(empresa, numeroCliente, "Não há horários livres em " + data.format(FORMATO_DATA) + ". " + MENSAGEM_PEDIR_DATA);
            return;
        }

        String s = sessao(empresa);
        gerenciadorSessao.getOuCriarReuniao(s, numeroCliente).setData(data);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_HORARIO);
        enviar(empresa, numeroCliente, "Horários livres em " + data.format(FORMATO_DATA) + ": " + formatar(livres)
                + ".\nQual horário você prefere? Envie no formato HH:MM.");
    }

    private void tratarHorario(Empresa empresa, String numeroCliente, String texto) {
        String s = sessao(empresa);
        DadosReuniaoSessao reuniao = gerenciadorSessao.getOuCriarReuniao(s, numeroCliente);
        LocalTime horario = lerHorario(texto);
        if (horario == null) {
            enviar(empresa, numeroCliente, "Não entendi o horário. Envie no formato HH:MM, por exemplo 14:00.");
            return;
        }

        List<LocalTime> livres;
        try {
            livres = horariosLivres(empresa, reuniao.getData());
        } catch (Exception e) {
            reuniaoFalhou(empresa, numeroCliente, e);
            return;
        }
        if (!livres.contains(horario)) {
            oferecerOutrosHorarios(empresa, numeroCliente, reuniao, livres);
            return;
        }

        reuniao.setHorario(horario);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_CONFIRMACAO);
        enviar(empresa, numeroCliente, "Confirma a reunião?\n"
                + "Motivo: " + reuniao.getMotivo().getRotulo() + "\n"
                + "Data: " + reuniao.getData().format(FORMATO_DATA) + "\n"
                + "Horário: " + horario.format(FORMATO_HORA) + "\n\n"
                + "1️⃣ - Sim, marcar\n2️⃣ - Não");
    }

    private void tratarConfirmacao(Empresa empresa, String numeroCliente, String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.equals("2") || normalizado.startsWith("nao")) {
            mostrarMenu(empresa, numeroCliente);
            return;
        }
        if (!(normalizado.equals("1") || normalizado.startsWith("sim"))) {
            enviar(empresa, numeroCliente, "Responda *1* para marcar a reunião ou *2* para cancelar.");
            return;
        }

        String s = sessao(empresa);
        DadosReuniaoSessao reuniao = gerenciadorSessao.getOuCriarReuniao(s, numeroCliente);
        try {
            List<LocalTime> livres = horariosLivres(empresa, reuniao.getData());
            if (!livres.contains(reuniao.getHorario())) {
                oferecerOutrosHorarios(empresa, numeroCliente, reuniao, livres);
                return;
            }
            int duracao = servicoFuncionamento.janelaDa(empresa, reuniao.getData()).duracaoMinutos();
            servicoReserva.agendarReuniao(
                    empresa,
                    reuniao.getData(),
                    reuniao.getHorario(),
                    duracao,
                    "Reunião Gamb - " + reuniao.getMotivo().getRotulo(),
                    "Reunião marcada pelo atendente da Gamb. WhatsApp: " + MascaraLog.numero(numeroCliente),
                    List.copyOf(empresa.getEmailsResponsaveisReuniao()),
                    UUID.randomUUID().toString());
        } catch (Exception e) {
            reuniaoFalhou(empresa, numeroCliente, e);
            return;
        }

        String resposta = "Reunião marcada para " + reuniao.getData().format(FORMATO_DATA)
                + " às " + reuniao.getHorario().format(FORMATO_HORA) + ".";
        gerenciadorSessao.limparReuniao(s, numeroCliente);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.INICIO);
        enviar(empresa, numeroCliente, resposta);
    }

    private void oferecerOutrosHorarios(Empresa empresa, String numeroCliente, DadosReuniaoSessao reuniao,
                                        List<LocalTime> livres) {
        String s = sessao(empresa);
        if (livres.isEmpty()) {
            gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_DATA);
            enviar(empresa, numeroCliente, "Não há mais horários livres em " + reuniao.getData().format(FORMATO_DATA)
                    + ". " + MENSAGEM_PEDIR_DATA);
            return;
        }
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.REUNIAO_HORARIO);
        enviar(empresa, numeroCliente, "Esse horário não está livre. Horários livres em "
                + reuniao.getData().format(FORMATO_DATA) + ": " + formatar(livres)
                + ".\nQual horário você prefere? Envie no formato HH:MM.");
    }

    private List<LocalTime> horariosLivres(Empresa empresa, LocalDate data) throws Exception {
        ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(empresa, data);
        LocalTime aPartirDe = data.equals(LocalDate.now(FUSO_BRASILIA)) ? LocalTime.now(FUSO_BRASILIA) : null;
        return servicoGoogleagenda.horariosLivres(
                empresa.getGoogleCalendarId(),
                data,
                ServicoFuncionamento.iniciosPossiveis(janela),
                janela.duracaoMinutos(),
                empresa.intervaloEfetivoMinutos(),
                aPartirDe);
    }

    private void reuniaoFalhou(Empresa empresa, String numeroCliente, Exception e) {
        log.warn("Falha ao consultar ou criar reunião na agenda da Gamb: {}", e.getClass().getSimpleName());
        String s = sessao(empresa);
        gerenciadorSessao.limparReuniao(s, numeroCliente);
        gerenciadorSessao.setEstado(s, numeroCliente, EstadoUsuario.ATENDENTE_MENU);
        enviar(empresa, numeroCliente, MENSAGEM_REUNIAO_INDISPONIVEL);
    }

    private LocalTime lerHorario(String texto) {
        Matcher m = PADRAO_HORA.matcher(texto.trim().toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        int hora = Integer.parseInt(m.group(1));
        int minuto = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        if (hora > 23 || minuto > 59) {
            return null;
        }
        return LocalTime.of(hora, minuto);
    }

    private String formatar(List<LocalTime> horarios) {
        return horarios.stream()
                .limit(MAXIMO_OPCOES)
                .map(h -> h.format(FORMATO_HORA))
                .collect(Collectors.joining(", "));
    }

    private String comando(String texto) {
        return normalizar(texto).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private void enviar(Empresa empresa, String numeroCliente, String texto) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, texto);
    }
}
