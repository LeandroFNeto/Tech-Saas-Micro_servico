package com.example.demo.strategy;

import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LocacaoStrategy implements ModuloAtendimentoStrategy {

    private static final Logger Log = LoggerFactory.getLogger(LocacaoStrategy.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_AGENDA = 15;

    @Autowired private ServicoMensagem servicoMensagem;
    @Autowired private GerenciadorSessao gerenciadorSessao;
    @Autowired private ServicoMenu servicoMenu;
    @Autowired private ServicoGoogleagenda servicoGoogleagenda;

    @Override
    public String getRamoDeAtuacao() {
        return "LOCACAO";
    }

    @Override
    public void processarMensagem(Empresa empresa, String numeroCliente, String textoRecebido, String estadoAtual) {
        String texto = textoRecebido == null ? "" : textoRecebido.trim();
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
            case RESERVA_ESPERANDO_NOME -> tratarReservaNome(empresa, numeroCliente, texto);
            case RESERVA_CONFIRMACAO -> tratarReservaConfirmacao(empresa, numeroCliente, texto);
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
                servicoMensagem.enviarMensagemWPP(
                        empresa, numeroCliente, "Por favor, me diga qual data você deseja verificar.");
                gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.ESPERANDO_DATA);
            }
            case "GOOGLE_CALENDAR" -> tratarAgendaProximosDias(empresa, numeroCliente);
            case "MENU_CARDAPIO" -> tratarCardapio(empresa, numeroCliente);
            case "IA_GEMINI" -> servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Recebemos sua mensagem. Logo um atendente vai te responder.");
            default -> servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Opção inválida. Digite o número de uma das opções do menu.");
        }
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
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Recebemos sua mensagem. Logo um atendente vai te responder.");
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
        if (!dataEstaLivreParaReserva(empresa, numeroCliente, data, texto)) {
            return;
        }

        DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
        reserva.setData(data);

        if (empresa.isLocacaoPorHora()) {
            servicoMensagem.enviarMensagemWPP(
                    empresa, numeroCliente, "Perfeito. Qual horário você deseja? Envie no formato HH:MM, por exemplo 14:00.");
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_ESPERANDO_HORARIO);
            return;
        }

        reserva.setHorario(DadosReservaSessao.HORARIO_DIARIA);
        pedirNome(empresa, numeroCliente);
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
        reserva.setHorario(hora.format(DateTimeFormatter.ofPattern("HH:mm")));
        pedirNome(empresa, numeroCliente);
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
        reserva.setNomeCliente(texto);
        enviarRegrasEPedirConfirmacao(empresa, numeroCliente, reserva);
    }

    private void enviarRegrasEPedirConfirmacao(Empresa empresa, String numeroCliente, DadosReservaSessao reserva) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, textoRegras(empresa));
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, montarResumoConfirmacao(reserva));
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.RESERVA_CONFIRMACAO);
    }

    private void tratarReservaConfirmacao(Empresa empresa, String numeroCliente, String texto) {
        String resposta = texto.toLowerCase(Locale.of("pt", "BR"));
        if (resposta.matches("sim|s|confirmar|1")) {
            DadosReservaSessao reserva = gerenciadorSessao.getOuCriarReserva(numeroCliente);
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Reserva confirmada para "
                            + formatarData(reserva.getData())
                            + " (" + valorOuTraco(reserva.getHorario()) + ") em nome de "
                            + valorOuTraco(reserva.getNomeCliente())
                            + ". Obrigado!");
            gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
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
        return "Confira os dados da reserva:\n"
                + "📅 Data: " + formatarData(reserva.getData()) + "\n"
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
            Log.warn("Falha ao consultar Google Agenda na reserva para {}: {}", textoOriginal, e.getMessage());
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
            Set<LocalDate> ocupadas = servicoGoogleagenda.datasOcupadas(
                    calendarId, hoje, hoje.plusDays(DIAS_AGENDA));
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, montarListaProximosDias(hoje, ocupadas));
        } catch (Exception e) {
            Log.warn("Falha ao listar agenda dos próximos {} dias: {}", DIAS_AGENDA, e.getMessage());
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Não consegui consultar a agenda agora. Tente novamente em instantes.");
        }
    }

    private String montarListaProximosDias(LocalDate hoje, Set<LocalDate> ocupadas) {
        Locale idiomaBrasil = Locale.of("pt", "BR");
        StringBuilder lista = new StringBuilder("📅 Agenda dos próximos 15 dias:\n");
        for (int i = 0; i < DIAS_AGENDA; i++) {
            LocalDate dia = hoje.plusDays(i);
            String nomeDia = dia.getDayOfWeek().getDisplayName(TextStyle.FULL, idiomaBrasil);
            String status = ocupadas.contains(dia) ? "❌ Alugado" : "✅ Livre";
            lista.append("\n")
                    .append(dia.format(FORMATO_DATA))
                    .append(" (")
                    .append(nomeDia)
                    .append(") - ")
                    .append(status);
        }
        return lista.toString();
    }

    private void tratarDataInformada(Empresa empresa, String numeroCliente, String texto) {
        Log.info("Buscando data: " + texto);
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

        try {
            boolean livre = servicoGoogleagenda.dataEstaLivre(calendarId, data);
            String resposta = livre
                    ? "✅ Esta data está livre para locação!"
                    : "❌ Infelizmente esta data já está reservada.";
            servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, resposta);
        } catch (Exception e) {
            Log.warn("Falha ao consultar Google Agenda para {}: {}", texto, e.getMessage());
            servicoMensagem.enviarMensagemWPP(
                    empresa,
                    numeroCliente,
                    "Não consegui consultar a agenda agora. Tente novamente em instantes.");
        }
        gerenciadorSessao.setEstado(numeroCliente, EstadoUsuario.INICIO);
    }
}
