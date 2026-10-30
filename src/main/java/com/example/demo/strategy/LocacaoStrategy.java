package com.example.demo.strategy;

import com.example.demo.evento.EventoReservaConfirmada;
import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class LocacaoStrategy implements ModuloAtendimentoStrategy {

    private static final Logger log = LoggerFactory.getLogger(LocacaoStrategy.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_AGENDA = 15;
    private static final String MENSAGEM_DATA_PASSADA =
            "Essa data já passou. Envie uma data de hoje em diante, no formato DD-MM-AAAA, por exemplo 16-09-2026.";
    private static final String MENSAGEM_HORARIO_PASSADO =
            "Esse horário de hoje já passou. Envie um horário que ainda não passou, no formato HH:MM, por exemplo 18:00.";
    private static final String MENSAGEM_RESERVA_MANUAL =
            "Recebemos sua mensagem. Logo um atendente vai te responder.";
    private static final String MENSAGEM_AINDA_REGISTRANDO =
            "Ainda estou registrando sua reserva, já te respondo.";
    private static final String LEMBRETE_VOLTAR_MENU =
            "🔄 *Lembrete:* Digite *0* a qualquer momento da conversa para voltar ao menu inicial.";

    @Autowired private ServicoMensagem servicoMensagem;
    @Autowired private GerenciadorSessao gerenciadorSessao;
    @Autowired private ServicoMenu servicoMenu;
    @Autowired private ServicoGoogleagenda servicoGoogleagenda;
    @Autowired private ServicoFuncionamento servicoFuncionamento;
    @Autowired private ApplicationEventPublisher publicadorEventos;

    @Override
    public String getRamoDeAtuacao() {
        return "LOCACAO";
    }

    @Override
    public void processarMensagem(Empresa empresa, String numeroCliente, String textoRecebido, String estadoAtual) {
        String texto = textoRecebido == null ? "" : textoRecebido.trim();
        if (gerenciadorSessao.getEstado(numeroCliente) == EstadoUsuario.RESERVA_REGISTRANDO) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, MENSAGEM_AINDA_REGISTRANDO);
            return;
        }
        if (ehComandoMenuPrincipal(texto)) {
            gerenciadorSessao.atualizarEstado(numeroCliente, EstadoUsuario.INICIO.name());
            tratarInicio(empresa, numeroCliente);
            return;
        }

        EstadoUsuario estado = gerenciadorSessao.getEstado(numeroCliente);
        if (estado == null) {
            estado = EstadoUsuario.INICIO;
        }

        switch (estado) {
            case INICIO -> tratarInicio(empresa, numeroCliente);
            case MENU_PRINCIPAL -> tratarMenuPrincipal(empresa, numeroCliente, texto);
            case ESPERANDO_DATA -> tratarDataInformada(empresa, numeroCliente, texto);
            case ESPERANDO_RESPOSTA_GALERIA -> tratarRespostaGaleria(empresa, numeroCliente, texto);
            case RESERVA_ESPERANDO_DATA -> tratarReservaData(empresa, numeroCliente, texto);
            case RESERVA_ESPERANDO_HORARIO -> tratarReservaHorario(empresa, numeroCliente, texto);
            case RESERVA_MAIS_DATAS -> tratarMaisDatas(empresa, numeroCliente, texto);
            case RESERVA_ESPERANDO_NOME -> tratarReservaNome(empresa, numeroCliente, texto);
            case RESERVA_CONFIRMACAO -> tratarReservaConfirmacao(empresa, numeroCliente, texto);
            case RESERVA_REGISTRANDO -> servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, MENSAGEM_AINDA_REGISTRANDO);
        }
    }

    private boolean ehComandoMenuPrincipal(String texto) {
        String normalizado = texto.toLowerCase(Locale.of("pt", "BR"));
        return "0".equals(normalizado) || "menu".equals(normalizado) || "voltar".equals(normalizado);
    }

    private void tratarInicio(Empresa empresa, String numeroCliente) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, servicoMenu.montarMenuPrincipal(empresa));
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.MENU_PRINCIPAL);
    }

    private void tratarMenuPrincipal(Empresa empresa, String numeroCliente, String texto) {
        String acao = servicoMenu.descobrirAcao(empresa, texto);
        if (acao == null) {
            acao = "OPCAO_INVALIDA";
        }

        switch (acao.toUpperCase(Locale.ROOT)) {
            case "VER_FOTOS" -> tratarFotos(empresa, numeroCliente);
            case "VER_LOCALIZACAO" -> tratarLocalizacao(empresa, numeroCliente);
            case "VER_REGRAS" -> tratarRegras(empresa, numeroCliente);
            case "SOLICITAR_RESERVA" -> tratarSolicitacaoReserva(empresa, numeroCliente);
            case "PESQUISAR_DATA" -> {
                servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, textoPedidoDataEspecifica());
                gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.ESPERANDO_DATA);
            }
            case "GOOGLE_CALENDAR" -> tratarAgendaProximosDias(empresa, numeroCliente);
            case "MENU_CARDAPIO" -> tratarCardapio(empresa, numeroCliente);
            case "IA_GEMINI" -> servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Recebemos sua mensagem. Logo um atendente vai te responder.");
            default -> {
                servicoMensagem.enviarMensagemWPP(
                        empresa, numeroCliente, "Opção inválida. Digite o número de uma das opções do menu.");
                return;
            }
        }
        enviarLembreteVoltarMenu(empresa, numeroCliente);
    }

    private String textoPedidoDataEspecifica() {
        return "Por favor, me diga qual data você deseja verificar.\n\n"
                + "💡 Digite a data no formato *DD/MM/AAAA* (Ex: 25/12/2026).";
    }

    private void enviarLembreteVoltarMenu(Empresa empresa, String numeroCliente) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, LEMBRETE_VOLTAR_MENU);
    }

    private void tratarFotos(Empresa empresa, String numeroCliente) {
        String fotoPrincipal = empresa.getLinkFotoPrincipal();
        if (fotoPrincipal != null && !fotoPrincipal.isBlank()) {
            servicoMensagem.enviarImagemWPP(empresa, numeroCliente, fotoPrincipal.trim(), null);
        } else {
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Ainda não cadastramos a foto principal deste espaço.");
        }
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, textoConviteGaleria(empresa));
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.ESPERANDO_RESPOSTA_GALERIA);
    }

    private String textoConviteGaleria(Empresa empresa) {
        String link = empresa.getLinkGaleria();
        StringBuilder texto = new StringBuilder("📸 Essa é a foto principal do nosso espaço!\n\n");
        if (link != null && !link.isBlank()) {
            texto.append("📂 *Quer ver a galeria completa com fotos e vídeos em alta qualidade?*\n")
                    .append("Acesse nosso Drive: ")
                    .append(link.trim())
                    .append("\n\n");
        }
        texto.append("👇 Deseja receber mais algumas fotos rápidas por aqui mesmo? (Responda *SIM* ou *NÃO*)");
        return texto.toString();
    }

    private void tratarRespostaGaleria(Empresa empresa, String numeroCliente, String texto) {
        String resposta = texto.toLowerCase(Locale.of("pt", "BR"));
        if (respostaAfirmativa(resposta)) {
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
            List<String> urls = List.copyOf(empresa.getUrlsGaleria());
            if (urls.isEmpty()) {
                servicoMensagem.enviarMensagemWPP(
                        empresa, numeroCliente, "Ainda não há fotos complementares cadastradas.");
                tratarInicio(empresa, numeroCliente);
                return;
            }
            servicoMensagem.enviarGaleriaComIntervalo(
                    empresa, numeroCliente, urls, () -> tratarInicio(empresa, numeroCliente));
            return;
        }
        if (respostaNegativa(resposta)) {
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
            tratarInicio(empresa, numeroCliente);
            return;
        }
        servicoMensagem.enviarMensagemWPP(
                empresa, numeroCliente, "Digite SIM para ver mais fotos ou NÃO para voltar ao menu.");
    }

    private boolean respostaAfirmativa(String texto) {
        return texto.matches("sim|s|quero|ss");
    }

    private boolean respostaNegativa(String texto) {
        return texto.matches("n[aã]o|n");
    }

    private void tratarLocalizacao(Empresa empresa, String numeroCliente) {
        if (empresa.getLinkGoogleMaps() == null || empresa.getLinkGoogleMaps().isBlank()) {
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Ainda não cadastramos o link da localização.");
            return;
        }
        servicoMensagem.enviarMensagemWPP(
                empresa, numeroCliente, "📍 Nossa localização:\n" + empresa.getLinkGoogleMaps().trim());
    }

    private void tratarRegras(Empresa empresa, String numeroCliente) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, textoRegras(empresa));
    }

    private void tratarCardapio(Empresa empresa, String numeroCliente) {
        if (empresa.getTabelaDePrecos() == null || empresa.getTabelaDePrecos().isBlank()) {
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "A tabela de preços ainda não foi cadastrada.");
            return;
        }
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, empresa.getTabelaDePrecos().trim());
    }

    private void tratarSolicitacaoReserva(Empresa empresa, String numeroCliente) {
        if (!Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica())) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, MENSAGEM_RESERVA_MANUAL);
            return;
        }
        if (empresa.getGoogleCalendarId() == null || empresa.getGoogleCalendarId().isBlank()) {
            log.warn("Empresa {} com reserva automática ligada e sem agenda; atendendo como reserva manual", empresa.getId());
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, MENSAGEM_RESERVA_MANUAL);
            return;
        }
        gerenciadorSessao.limparReserva(numeroCliente);
        gerenciadorSessao.getOuCriarReserva(numeroCliente);
        servicoMensagem.enviarMensagemWPP(
                empresa, numeroCliente, "Por favor, me diga qual data você deseja reservar.");
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_DATA);
    }

    private void tratarReservaData(Empresa empresa, String numeroCliente, String texto) {
        LocalDate data = parseData(empresa, numeroCliente, texto);
        if (data == null) {
            return;
        }
        if (recusarDataPassada(empresa, numeroCliente, data)) {
            return;
        }
        if (empresa.isLocacaoPorHora()) {
            tratarReservaDataPorHora(empresa, numeroCliente, data);
            return;
        }
        if (!dataEstaLivreParaReserva(empresa, numeroCliente, data, texto)) {
            return;
        }

        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
        reserva.setData(data);
        reserva.setHorario(DadosReservaSessao.HORARIO_DIARIA);
        pedirNome(empresa, numeroCliente);
    }

    private void tratarReservaDataPorHora(Empresa empresa, String numeroCliente, LocalDate data) {
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
        LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
        if (data.isAfter(hoje.plusDays(ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE))) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                    "Só aceitamos reservas até " + ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE
                            + " dias à frente. Envie uma data mais próxima.");
            return;
        }
        if (reserva.getDatas().contains(data)) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, "Essa data já está no seu pedido.");
            perguntarMaisDatas(empresa, numeroCliente, reserva);
            return;
        }

        ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(empresa, data);
        if (!janela.aberto()) {
            informarDataFechada(empresa, numeroCliente, data, janela);
            return;
        }

        if (!reserva.getDatas().isEmpty() && reserva.getHorario() != null) {
            LocalTime hora = LocalTime.parse(reserva.getHorario());
            String problema = problemaDoHorario(empresa, data, janela, hora);
            if (problema != null) {
                servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                        problema + "\nEnvie outra data, no formato DD-MM-AAAA.");
                return;
            }
            reserva.adicionarData(data);
            perguntarMaisDatas(empresa, numeroCliente, reserva);
            return;
        }

        List<LocalTime> livres;
        try {
            livres = horariosLivresDoDia(empresa, data, janela);
        } catch (Exception e) {
            log.warn("Falha ao consultar Google Agenda na reserva por hora: {}", e.getClass().getSimpleName());
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                    "Não consegui consultar a agenda agora. Tente novamente em instantes.");
            return;
        }
        if (livres.isEmpty()) {
            StringBuilder mensagem = new StringBuilder("❌ Não há horários livres em " + data.format(FORMATO_DATA) + ".");
            List<LocalDate> proximas = sugerirDatasContextuais(empresa, data);
            if (proximas != null && !proximas.isEmpty()) {
                mensagem.append(" Próximas datas abertas: ")
                        .append(proximas.stream().map(d -> d.format(FORMATO_DATA)).collect(Collectors.joining(", ")))
                        .append(".");
            }
            mensagem.append(" Envie outra data, no formato DD-MM-AAAA.");
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, mensagem.toString());
            return;
        }
        reserva.setData(data);
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                "Horários livres em " + data.format(FORMATO_DATA) + ":\n" + formatarHorarios(livres)
                        + "\n\nQual horário você deseja? Envie no formato HH:MM, por exemplo "
                        + livres.get(0).format(DateTimeFormatter.ofPattern("HH:mm")) + ".");
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
    }

    private void informarDataFechada(Empresa empresa, String numeroCliente, LocalDate data,
                                     ServicoFuncionamento.Janela janela) {
        StringBuilder mensagem = new StringBuilder("Não abrimos em " + data.format(FORMATO_DATA));
        if (janela.motivo() != null && !janela.motivo().isBlank()) {
            mensagem.append(" (").append(janela.motivo()).append(")");
        }
        mensagem.append(".");
        List<LocalDate> proximas = sugerirDatasContextuais(empresa, data);
        if (proximas != null && !proximas.isEmpty()) {
            mensagem.append(" Próximas datas abertas: ")
                    .append(proximas.stream().map(d -> d.format(FORMATO_DATA)).collect(Collectors.joining(", ")))
                    .append(".");
        }
        mensagem.append(" Envie outra data, no formato DD-MM-AAAA.");
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, mensagem.toString());
    }

    private List<LocalTime> horariosLivresDoDia(Empresa empresa, LocalDate data, ServicoFuncionamento.Janela janela)
            throws Exception {
        LocalTime aPartirDe = data.equals(LocalDate.now(FUSO_BRASILIA)) ? LocalTime.now(FUSO_BRASILIA) : null;
        return servicoGoogleagenda.horariosLivres(
                empresa.getGoogleCalendarId(),
                data,
                ServicoFuncionamento.iniciosPossiveis(janela),
                janela.duracaoMinutos(),
                empresa.intervaloEfetivoMinutos(),
                aPartirDe);
    }

    private String problemaDoHorario(Empresa empresa, LocalDate data, ServicoFuncionamento.Janela janela, LocalTime hora) {
        if (!janela.aberto()) {
            return "Não abrimos em " + data.format(FORMATO_DATA) + ".";
        }
        if (data.equals(LocalDate.now(FUSO_BRASILIA)) && hora.isBefore(LocalTime.now(FUSO_BRASILIA))) {
            return MENSAGEM_HORARIO_PASSADO;
        }
        List<LocalTime> inicios = ServicoFuncionamento.iniciosPossiveis(janela);
        if (!inicios.contains(hora)) {
            LocalTime maisProximo = inicioMaisProximo(inicios, hora);
            String detalheProximo = maisProximo != null
                    ? " O início mais próximo é " + maisProximo.format(DateTimeFormatter.ofPattern("HH:mm")) + "."
                    : "";
            return "Esse horário não está disponível em " + data.format(FORMATO_DATA) + "."
                    + detalheProximo + " Horários de início: " + formatarHorarios(inicios) + ".";
        }
        try {
            boolean livre = servicoGoogleagenda.horarioEstaLivre(
                    empresa.getGoogleCalendarId(), data, hora, hora.plusMinutes(janela.duracaoMinutos()),
                    empresa.intervaloEfetivoMinutos());
            if (!livre) {
                return "❌ Esse horário já está reservado em " + data.format(FORMATO_DATA) + ".";
            }
        } catch (Exception e) {
            log.warn("Falha ao consultar Google Agenda na reserva por hora: {}", e.getClass().getSimpleName());
            return "Não consegui consultar a agenda agora. Tente novamente em instantes.";
        }
        return null;
    }

    static LocalTime inicioMaisProximo(List<LocalTime> inicios, LocalTime hora) {
        if (inicios == null || inicios.isEmpty() || hora == null) {
            return null;
        }
        LocalTime maisProximo = null;
        long menorDiferenca = Long.MAX_VALUE;
        for (LocalTime inicio : inicios) {
            long dif = Math.abs(Duration.between(inicio, hora).toMinutes());
            if (dif < menorDiferenca) {
                menorDiferenca = dif;
                maisProximo = inicio;
            }
        }
        return maisProximo;
    }

    List<LocalDate> sugerirDatasContextuais(Empresa empresa, LocalDate data) {
        LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
        List<LocalDate> candidatas = new ArrayList<>();
        DayOfWeek diaSemana = data.getDayOfWeek();
        if (diaSemana == DayOfWeek.FRIDAY) {
            candidatas.add(data.plusDays(1));
            candidatas.add(data.plusDays(2));
            candidatas.add(data.plusDays(7));
        } else if (diaSemana == DayOfWeek.SATURDAY) {
            candidatas.add(data.plusDays(1));
            candidatas.add(data.plusDays(7));
            candidatas.add(data.plusDays(6));
        } else if (diaSemana == DayOfWeek.SUNDAY) {
            candidatas.add(data.plusWeeks(1));
            if (data.isAfter(hoje.plusWeeks(2))) {
                LocalDate domingoAnterior = data.minusWeeks(1);
                if (!domingoAnterior.isBefore(hoje)) {
                    candidatas.add(domingoAnterior);
                }
            }
            candidatas.add(data.plusWeeks(2));
        } else {
            candidatas.add(data.plusDays(1));
            candidatas.add(data.plusDays(7));
        }

        List<LocalDate> resultado = new ArrayList<>();
        LocalDate limiteMaximo = hoje.plusDays(ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE);
        for (LocalDate d : candidatas) {
            if (!d.isBefore(hoje) && !d.isAfter(limiteMaximo) && !resultado.contains(d)) {
                try {
                    ServicoFuncionamento.Janela j = servicoFuncionamento.janelaDa(empresa, d);
                    if (j != null && j.aberto()) {
                        resultado.add(d);
                    }
                } catch (Exception ignored) {
                }
            }
            if (resultado.size() == 3) {
                break;
            }
        }
        if (resultado.size() < 3) {
            List<LocalDate> fallback = servicoFuncionamento.proximasDatasAbertas(empresa, data.plusDays(1), 3);
            if (fallback != null) {
                for (LocalDate f : fallback) {
                    if (!resultado.contains(f)) {
                        resultado.add(f);
                    }
                    if (resultado.size() == 3) {
                        break;
                    }
                }
            }
        }
        return resultado;
    }

    private String formatarHorarios(List<LocalTime> horarios) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        return horarios.stream().map(h -> h.format(formato)).collect(Collectors.joining(", "));
    }

    private void tratarReservaHorario(Empresa empresa, String numeroCliente, String texto) {
        LocalTime hora;
        try {
            hora = parseHorario(texto);
        } catch (DateTimeParseException e) {
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Não entendi esse horário. Envie no formato HH:MM, por exemplo 14:00.");
            return;
        }
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
        LocalDate dataReserva = reserva.getData();
        if (dataReserva != null && dataReserva.equals(LocalDate.now(FUSO_BRASILIA))
                && hora.isBefore(LocalTime.now(FUSO_BRASILIA))) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, MENSAGEM_HORARIO_PASSADO);
            return;
        }
        if (dataReserva == null) {
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_DATA);
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, "Por favor, me diga qual data você deseja reservar.");
            return;
        }
        ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(empresa, dataReserva);
        String problema = problemaDoHorario(empresa, dataReserva, janela, hora);
        if (problema != null) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, problema);
            return;
        }
        reserva.setHorario(hora.format(DateTimeFormatter.ofPattern("HH:mm")));
        reserva.adicionarData(dataReserva);
        perguntarMaisDatas(empresa, numeroCliente, reserva);
    }

    private void perguntarMaisDatas(Empresa empresa, String numeroCliente, DadosReservaSessao reserva) {
        if (reserva.atingiuLimiteDeDatas()) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                    "Chegamos ao limite de " + DadosReservaSessao.MAXIMO_DATAS + " datas por pedido.");
            pedirNome(empresa, numeroCliente);
            return;
        }
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                "✅ Anotado: " + listarDatas(reserva) + " às " + reserva.getHorario() + ".\n\n"
                        + "Quer reservar mais alguma data no mesmo horário? Responda *SIM* ou *NÃO*.\n"
                        + "Para tirar uma data, envie *TIRAR DD-MM-AAAA*.");
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_MAIS_DATAS);
    }

    private void tratarMaisDatas(Empresa empresa, String numeroCliente, String texto) {
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
        String resposta = texto.toLowerCase(Locale.of("pt", "BR"));
        if (respostaAfirmativa(resposta)) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                    "Qual a próxima data? Envie no formato DD-MM-AAAA, por exemplo 16-09-2026.");
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_DATA);
            return;
        }
        if (respostaNegativa(resposta)) {
            pedirNome(empresa, numeroCliente);
            return;
        }
        if (tentarTirarData(empresa, numeroCliente, reserva, resposta)) {
            return;
        }
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                "Digite SIM para incluir outra data, NÃO para seguir ou TIRAR DD-MM-AAAA para remover uma data.");
    }

    private boolean tentarTirarData(Empresa empresa, String numeroCliente, DadosReservaSessao reserva, String resposta) {
        if (!resposta.startsWith("tirar")) {
            return false;
        }
        LocalDate alvo;
        try {
            alvo = LocalDate.parse(resposta.substring(5).trim().replace("/", "-"), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                    "Não entendi qual data tirar. Envie TIRAR DD-MM-AAAA.");
            return true;
        }
        if (!reserva.removerData(alvo)) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, "Essa data não está no seu pedido.");
            return true;
        }
        if (reserva.getDatas().isEmpty()) {
            reserva.setHorario(null);
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente,
                    "Sem datas no pedido. Envie uma data, no formato DD-MM-AAAA.");
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_DATA);
            return true;
        }
        perguntarMaisDatas(empresa, numeroCliente, reserva);
        return true;
    }

    private String listarDatas(DadosReservaSessao reserva) {
        return reserva.getDatas().stream().map(d -> d.format(FORMATO_DATA)).collect(Collectors.joining(", "));
    }

    private boolean recusarDataPassada(Empresa empresa, String numeroCliente, LocalDate data) {
        if (!data.isBefore(LocalDate.now(FUSO_BRASILIA))) {
            return false;
        }
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, MENSAGEM_DATA_PASSADA);
        return true;
    }

    private void pedirNome(Empresa empresa, String numeroCliente) {
        servicoMensagem.enviarMensagemWPP(
                empresa, numeroCliente, "Para seguir, me diga o nome do responsável pela reserva.");
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_NOME);
    }

    private void tratarReservaNome(Empresa empresa, String numeroCliente, String texto) {
        if (texto.isBlank()) {
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Preciso do nome do responsável para continuar.");
            return;
        }
        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
        String nome = texto.replaceAll("\\s+", " ").trim();
        reserva.setNomeCliente(nome.length() > 80 ? nome.substring(0, 80) : nome);
        enviarRegrasEPedirConfirmacao(empresa, numeroCliente, reserva);
    }

    private void enviarRegrasEPedirConfirmacao(Empresa empresa, String numeroCliente, DadosReservaSessao reserva) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, textoRegras(empresa));
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, montarResumoConfirmacao(reserva));
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_CONFIRMACAO);
    }

    private void tratarReservaConfirmacao(Empresa empresa, String numeroCliente, String texto) {
        String resposta = texto.toLowerCase(Locale.of("pt", "BR"));
        if (resposta.startsWith("tirar") && empresa.isLocacaoPorHora()
                && tentarTirarData(empresa, numeroCliente, gerenciadorSessao.getOuCriarReserva(numeroCliente), resposta)) {
            return;
        }
        if (resposta.matches("sim|s|confirmar|1")) {
            DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
            boolean porHora = empresa.isLocacaoPorHora() && !reserva.getDatas().isEmpty();
            String data = porHora ? listarDatas(reserva) : formatarData(reserva.getData());
            List<String> datasEvento = porHora
                    ? reserva.getDatas().stream().map(d -> d.format(FORMATO_DATA)).toList()
                    : List.of(data);
            EventoReservaConfirmada evento = new EventoReservaConfirmada(
                    empresa,
                    datasEvento,
                    reserva.getHorario(),
                    reserva.getNomeCliente(),
                    numeroCliente,
                    UUID.randomUUID().toString(),
                    porHora);
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_REGISTRANDO);
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Anotei seu pedido para " + data + ". Estou registrando na agenda e já confirmo.");
            publicadorEventos.publishEvent(evento);
            return;
        }
        if (resposta.matches("n[aã]o|n|cancelar|2")) {
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, "Reserva cancelada. Se quiser, é só chamar de novo.");
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
            return;
        }
        servicoMensagem.enviarMensagemWPP(
                empresa, numeroCliente, "Digite SIM para confirmar ou NÃO para cancelar.");
    }

    private String textoRegras(Empresa empresa) {
        String regras = empresa.getRegrasLocacao();
        if (regras == null || regras.isBlank()) {
            return "📋 Regras do local e cancelamento ainda não foram cadastradas.";
        }
        return "📋 Regras do local e cancelamento:\n" + regras.trim();
    }

    private String montarResumoConfirmacao(DadosReservaSessao reserva) {
        String linhaData = reserva.getDatas().size() > 1
                ? "📅 Datas: " + listarDatas(reserva)
                : "📅 Data: " + formatarData(reserva.getData());
        return "Confira os dados da reserva:\n"
                + linhaData + "\n"
                + "⏰ Horário: " + valorOuTraco(reserva.getHorario()) + "\n"
                + "👤 Nome: " + valorOuTraco(reserva.getNomeCliente())
                + "\n\nDigite SIM para confirmar ou NÃO para cancelar.";
    }

    private String formatarData(LocalDate data) {
        return data == null ? "-" : data.format(FORMATO_DATA);
    }

    private String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private LocalDate parseData(Empresa empresa, String numeroCliente, String texto) {
        try {
            return LocalDate.parse(texto.replace("/", "-"), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Não entendi essa data. Envie no formato DD-MM-AAAA, por exemplo 16-09-2026.");
            return null;
        }
    }

    private LocalTime parseHorario(String texto) {
        String normalizado = texto.trim().toLowerCase(Locale.ROOT).replace("h", ":");
        if (normalizado.matches("\\d{1,2}")) {
            normalizado = normalizado + ":00";
        }
        return LocalTime.parse(normalizado, FORMATO_HORA);
    }

    private boolean dataEstaLivreParaReserva(Empresa empresa, String numeroCliente, LocalDate data, String textoOriginal) {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            return true;
        }
        try {
            if (!servicoGoogleagenda.dataEstaLivre(calendarId, data)) {
                servicoMensagem.enviarMensagemWPP(
                        empresa, numeroCliente, "❌ Infelizmente esta data já está reservada.");
                gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Falha ao consultar Google Agenda na reserva: {}", e.getClass().getSimpleName());
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Não consegui consultar a agenda agora. Tente novamente em instantes.");
            return false;
        }
    }

    private void tratarAgendaProximosDias(Empresa empresa, String numeroCliente) {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "O calendário ainda não foi configurado. Peça ao administrador para cadastrar o ID da agenda.");
            return;
        }

        try {
            LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
            if (empresa.isLocacaoPorHora()) {
                List<ServicoGoogleagenda.Intervalo> ocupados = servicoGoogleagenda.ocupacoesDoPeriodo(
                        calendarId, hoje, hoje.plusDays(DIAS_AGENDA));
                servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, montarListaProximosDias(hoje, dia -> {
                    ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(empresa, dia);
                    return switch (servicoFuncionamento.situacaoDoDia(empresa, dia, janela, ocupados)) {
                        case FECHADO -> "🚫 Fechado";
                        case LOTADO -> "❌ Lotado";
                        case LIVRE -> "✅ Livre";
                    };
                }));
                return;
            }
            Set<LocalDate> ocupadas = servicoGoogleagenda.datasOcupadas(
                    calendarId, hoje, hoje.plusDays(DIAS_AGENDA));
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, montarListaProximosDias(hoje,
                    dia -> ocupadas.contains(dia) ? "❌ Alugado" : "✅ Livre"));
        } catch (Exception e) {
            log.warn("Falha ao listar agenda dos próximos {} dias: {}", DIAS_AGENDA, e.getClass().getSimpleName());
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Não consegui consultar a agenda agora. Tente novamente em instantes.");
        }
    }

    private String montarListaProximosDias(LocalDate hoje, Function<LocalDate, String> statusDoDia) {
        Locale idiomaBrasil = Locale.of("pt", "BR");
        StringBuilder lista = new StringBuilder("📅 Agenda dos próximos 15 dias:\n");
        for (int i = 0; i < DIAS_AGENDA; i++) {
            LocalDate dia = hoje.plusDays(i);
            String nomeDia = dia.getDayOfWeek().getDisplayName(TextStyle.FULL, idiomaBrasil);
            String status = statusDoDia.apply(dia);
            lista.append("\n")
                    .append(dia.format(FORMATO_DATA))
                    .append(" (")
                    .append(nomeDia)
                    .append(") - ")
                    .append(status);
        }
        return lista.toString();
    }

    private String respostaDaDataPorHora(Empresa empresa, String calendarId, LocalDate data) throws Exception {
        ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(empresa, data);
        if (!janela.aberto()) {
            return "🚫 Não abrimos nesta data (" + janela.motivo() + ").";
        }
        List<ServicoGoogleagenda.Intervalo> ocupados = servicoGoogleagenda.ocupacoesDoDia(calendarId, data);
        List<LocalTime> livres = servicoFuncionamento.iniciosLivres(empresa, data, janela, ocupados);
        if (livres.isEmpty()) {
            return "❌ Todos os horários desta data já estão reservados.";
        }
        return "✅ Há horários livres nesta data: " + livres.stream()
                .map(h -> h.format(FORMATO_HORA)).collect(Collectors.joining(", "));
    }

    private void tratarDataInformada(Empresa empresa, String numeroCliente, String texto) {
        log.info("Buscando data informada pelo cliente.");
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "O calendário ainda não foi configurado. Peça ao administrador para cadastrar o ID da agenda.");
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
            return;
        }

        LocalDate data = parseData(empresa, numeroCliente, texto);
        if (data == null) {
            return;
        }
        if (recusarDataPassada(empresa, numeroCliente, data)) {
            return;
        }

        try {
            String resposta;
            if (empresa.isLocacaoPorHora()) {
                resposta = respostaDaDataPorHora(empresa, calendarId, data);
            } else {
                resposta = servicoGoogleagenda.dataEstaLivre(calendarId, data)
                        ? "✅ Esta data está livre para locação!"
                        : "❌ Infelizmente esta data já está reservada.";
            }
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, resposta);
        } catch (Exception e) {
            log.warn("Falha ao consultar Google Agenda: {}", e.getClass().getSimpleName());
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Não consegui consultar a agenda agora. Tente novamente em instantes.");
        }
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
    }
}
