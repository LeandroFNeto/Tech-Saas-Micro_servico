package com.example.demo.servico;

import com.example.demo.dto.DiaReservaDTO;
import com.example.demo.model.Empresa;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ServicoReserva {

    private static final Logger log = LoggerFactory.getLogger(ServicoReserva.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final String CHAVE_REUNIAO_GAMB = "reuniaoGamb";
    private static final String VALOR_REUNIAO_GAMB = "true";
    public static final String MARCA_REUNIAO_GAMB = CHAVE_REUNIAO_GAMB + "=" + VALOR_REUNIAO_GAMB;
    private static final String ALFABETO_BASE32HEX = "0123456789abcdefghijklmnopqrstuv";

    @Autowired
    private ServicoGoogleagenda agendaService;

    @Autowired
    private ServicoFuncionamento servicoFuncionamento;

    public String agendarNoGoogle(Empresa empresa, String dataReserva, String nomeCliente, String whatsappId, String idPedido) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            log.warn("Empresa {} sem agenda cadastrada", empresa.getId());
            throw new AgendaNaoConfiguradaException(empresa.getId());
        }

        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        LocalDate dia = LocalDate.parse(dataReserva, FORMATO_DATA);

        Event evento = new Event()
                .setId(idEventoDoPedido(idPedido))
                .setSummary("Reserva - " + nomeCliente)
                .setDescription("Reserva feita pelo bot. WhatsApp: " + whatsappId)
                .setStart(new EventDateTime().setDate(new DateTime(dia.toString())))
                .setEnd(new EventDateTime().setDate(new DateTime(dia.plusDays(1).toString())));

        Event criado = servicoAgenda.events().insert(calendarId, evento).execute();
        log.info("Reserva salva na agenda da empresa {}", empresa.getId());
        return criado.getId();
    }

    public String agendarPorHora(Empresa empresa, LocalDate dia, LocalTime inicio, LocalTime fim, String nomeCliente,
                                 String whatsappId, String idPedido, int posicao) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            log.warn("Empresa {} sem agenda cadastrada", empresa.getId());
            throw new AgendaNaoConfiguradaException(empresa.getId());
        }

        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        String fusoTexto = FUSO_BRASILIA.getId();

        Event evento = new Event()
                .setId(idEventoDoPedido(idDaPosicao(idPedido, posicao)))
                .setSummary("Reserva - " + nomeCliente)
                .setDescription("Reserva feita pelo bot. WhatsApp: " + whatsappId)
                .setStart(new EventDateTime()
                        .setDateTime(new DateTime(dia.atTime(inicio).atZone(FUSO_BRASILIA).toInstant().toEpochMilli()))
                        .setTimeZone(fusoTexto))
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime((fim.equals(LocalTime.MIDNIGHT) ? dia.plusDays(1).atStartOfDay() : dia.atTime(fim)).atZone(FUSO_BRASILIA).toInstant().toEpochMilli()))
                        .setTimeZone(fusoTexto));

        Event criado = servicoAgenda.events().insert(calendarId, evento).execute();
        log.info("Reserva por hora salva na agenda da empresa {}", empresa.getId());
        return criado.getId();
    }

    public String agendarReuniao(Empresa empresa, LocalDate dia, LocalTime inicio, int duracaoMinutos, String titulo,
                                 String descricao, List<String> emailsConvidados, String idReuniao) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(empresa.getId());
        }

        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        String fusoTexto = FUSO_BRASILIA.getId();
        LocalDateTime comeco = dia.atTime(inicio);

        List<EventAttendee> convidados = new ArrayList<>();
        for (String email : emailsConvidados) {
            convidados.add(new EventAttendee().setEmail(email));
        }

        Event evento = new Event()
                .setId(idEventoDoPedido(idReuniao))
                .setSummary(titulo)
                .setDescription(descricao)
                .setAttendees(convidados)
                .setExtendedProperties(new Event.ExtendedProperties()
                        .setPrivate(Map.of(CHAVE_REUNIAO_GAMB, VALOR_REUNIAO_GAMB)))
                .setStart(new EventDateTime()
                        .setDateTime(new DateTime(comeco.atZone(FUSO_BRASILIA).toInstant().toEpochMilli()))
                        .setTimeZone(fusoTexto))
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime(comeco.plusMinutes(duracaoMinutos).atZone(FUSO_BRASILIA).toInstant().toEpochMilli()))
                        .setTimeZone(fusoTexto));

        Event criado = servicoAgenda.events().insert(calendarId, evento).setSendUpdates("all").execute();
        log.info("Reunião da Gamb salva na agenda da empresa {} com {} convidados", empresa.getId(), convidados.size());
        return criado.getId();
    }

    public int atualizarConvidadosDeReunioesFuturas(Empresa empresa, List<String> emailsConvidados) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            return 0;
        }

        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        Events eventos = servicoAgenda.events().list(calendarId)
                .setTimeMin(new DateTime(System.currentTimeMillis()))
                .setSingleEvents(true)
                .setPrivateExtendedProperty(List.of(MARCA_REUNIAO_GAMB))
                .setMaxResults(250)
                .execute();

        if (eventos == null || eventos.getItems() == null) {
            return 0;
        }

        int atualizadas = 0;
        for (Event evento : eventos.getItems()) {
            if ("cancelled".equalsIgnoreCase(evento.getStatus())) {
                continue;
            }
            List<EventAttendee> convidados = convidadosAtualizados(evento.getAttendees(), emailsConvidados);
            servicoAgenda.events()
                    .patch(calendarId, evento.getId(), new Event().setAttendees(convidados))
                    .setSendUpdates("all")
                    .execute();
            atualizadas++;
        }
        log.info("Convidados atualizados em {} reuniões futuras da empresa {}", atualizadas, empresa.getId());
        return atualizadas;
    }

    static List<EventAttendee> convidadosAtualizados(List<EventAttendee> atuais, List<String> emailsConvidados) {
        Map<String, EventAttendee> porEmail = new LinkedHashMap<>();
        if (atuais != null) {
            for (EventAttendee atual : atuais) {
                if (atual.getEmail() != null) {
                    porEmail.put(atual.getEmail().toLowerCase(Locale.ROOT), atual);
                }
            }
        }
        List<EventAttendee> resultado = new ArrayList<>();
        for (String email : emailsConvidados) {
            EventAttendee existente = porEmail.get(email.toLowerCase(Locale.ROOT));
            resultado.add(existente != null ? existente : new EventAttendee().setEmail(email));
        }
        return resultado;
    }

    public void removerEventoPorHora(Empresa empresa, String idPedido, int posicao) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(empresa.getId());
        }
        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        try {
            servicoAgenda.events().delete(calendarId, idEventoDoPedido(idDaPosicao(idPedido, posicao))).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() != 404 && e.getStatusCode() != 410) {
                throw e;
            }
        }
    }

    public boolean eventoPorHoraExiste(Empresa empresa, String idPedido, int posicao) throws Exception {
        return eventoDoPedidoExiste(empresa, idDaPosicao(idPedido, posicao));
    }

    public static String idDaPosicao(String idPedido, int posicao) {
        return idPedido + "#" + posicao;
    }

    public boolean eventoDoPedidoExiste(Empresa empresa, String idPedido) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(empresa.getId());
        }
        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        try {
            Event existente = servicoAgenda.events().get(calendarId, idEventoDoPedido(idPedido)).execute();
            return existente != null && !"cancelled".equalsIgnoreCase(existente.getStatus());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    public record EventoAgendaDTO(
            String id,
            String titulo,
            String descricao,
            LocalDate data,
            String horario,
            String status
    ) {}

    public List<EventoAgendaDTO> listarEventosProximos(Empresa empresa, int dias) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            return Collections.emptyList();
        }
        com.google.api.services.calendar.Calendar servicoGoogle = agendaService.conectarAgenda();
        LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
        LocalDate fim = hoje.plusDays(dias);

        DateTime timeMin = new DateTime(hoje.atStartOfDay(FUSO_BRASILIA).toInstant().toEpochMilli());
        DateTime timeMax = new DateTime(fim.plusDays(1).atStartOfDay(FUSO_BRASILIA).toInstant().toEpochMilli());

        Events eventos = servicoGoogle.events().list(calendarId)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setSingleEvents(true)
                .setShowDeleted(true)
                .execute();

        if (eventos.getItems() == null) {
            return Collections.emptyList();
        }

        List<EventoAgendaDTO> resultado = new ArrayList<>();
        for (Event item : eventos.getItems()) {
            String status = item.getStatus();
            LocalDate dataEvento = null;
            String horario = "Diária Completa";

            if (item.getStart() != null) {
                if (item.getStart().getDate() != null) {
                    dataEvento = LocalDate.parse(item.getStart().getDate().toStringRfc3339().substring(0, 10));
                    horario = "Diária Completa";
                } else if (item.getStart().getDateTime() != null) {
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(item.getStart().getDateTime().getValue()), FUSO_BRASILIA);
                    dataEvento = zdt.toLocalDate();
                    horario = zdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
            }

            if (dataEvento != null) {
                resultado.add(new EventoAgendaDTO(
                        item.getId(),
                        item.getSummary(),
                        item.getDescription(),
                        dataEvento,
                        horario,
                        status
                ));
            }
        }
        return resultado;
    }

    public static String idEventoDoPedido(String idPedido) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(idPedido.getBytes(StandardCharsets.UTF_8));
            StringBuilder id = new StringBuilder();
            int buffer = 0;
            int bits = 0;
            for (byte b : hash) {
                buffer = (buffer << 8) | (b & 0xFF);
                bits += 8;
                while (bits >= 5) {
                    id.append(ALFABETO_BASE32HEX.charAt((buffer >> (bits - 5)) & 31));
                    bits -= 5;
                }
                buffer &= (1 << bits) - 1;
            }
            if (bits > 0) {
                id.append(ALFABETO_BASE32HEX.charAt((buffer << (5 - bits)) & 31));
            }
            return id.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<DiaReservaDTO> listarReservas(Empresa empresa) {
        try {
            com.google.api.services.calendar.Calendar servicoGoogle = agendaService.conectarAgenda();
            DateTime agora = new DateTime(System.currentTimeMillis());

            String calendarId = empresa.getGoogleCalendarId();
            if (calendarId == null) {
                return null;
            }

            if (empresa.isLocacaoPorHora()) {
                return listarReservasPorHora(empresa, calendarId);
            }

            Events eventos = servicoGoogle.events().list(calendarId)
                    .setMaxResults(100)
                    .setTimeMin(agora)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> itens = eventos.getItems() == null ? List.of() : eventos.getItems();

            List<DiaReservaDTO> listaDias = new ArrayList<>();
            Set<LocalDate> diasAlugados = new HashSet<>();

            for (Event evento : itens) {
                if ("cancelled".equalsIgnoreCase(evento.getStatus())) {
                    continue;
                }
                EventDateTime inicio = evento.getStart();
                if (inicio != null) {
                    if (inicio.getDateTime() != null) {
                        LocalDate diaComHora = Instant.ofEpochMilli(inicio.getDateTime().getValue())
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(FUSO_BRASILIA)
                                .toLocalDate();
                        diasAlugados.add(diaComHora);
                    } else if (inicio.getDate() != null) {
                        LocalDate diaInicio = LocalDate.parse(inicio.getDate().toStringRfc3339().substring(0, 10));
                        EventDateTime fim = evento.getEnd();
                        LocalDate diaFim = (fim != null && fim.getDate() != null)
                                ? LocalDate.parse(fim.getDate().toStringRfc3339().substring(0, 10))
                                : diaInicio.plusDays(1);
                        if (!diaFim.isAfter(diaInicio)) {
                            diaFim = diaInicio.plusDays(1);
                        }
                        for (LocalDate d = diaInicio; d.isBefore(diaFim); d = d.plusDays(1)) {
                            diasAlugados.add(d);
                        }
                    }
                }
            }

            DateTimeFormatter formatadorDia = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
            Locale idiomaBrasil = Locale.of("pt", "BR");

            for (int i = 0; i < 30; i++) {
                LocalDate diaAtual = hoje.plusDays(i);
                String dataFormatada = diaAtual.format(formatadorDia);
                String nomeDiaSemana = diaAtual.getDayOfWeek().getDisplayName(TextStyle.FULL, idiomaBrasil);

                if (diasAlugados.contains(diaAtual)) {
                    listaDias.add(new DiaReservaDTO(dataFormatada, nomeDiaSemana, "Alugado"));
                } else {
                    listaDias.add(new DiaReservaDTO(dataFormatada, nomeDiaSemana, "Disponível"));
                }
            }
            return listaDias;

        } catch (Exception e) {
            log.error("Erro no Google Calendar ao listar (empresa {}): {}", empresa.getId(), e.getClass().getSimpleName());
            return null;
        }
    }

    private List<DiaReservaDTO> listarReservasPorHora(Empresa empresa, String calendarId) throws Exception {
        LocalDate hoje = LocalDate.now(FUSO_BRASILIA);
        List<ServicoGoogleagenda.Intervalo> ocupados = agendaService.ocupacoesDoPeriodo(calendarId, hoje, hoje.plusDays(30));
        DateTimeFormatter formatadorDia = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Locale idiomaBrasil = Locale.of("pt", "BR");
        List<DiaReservaDTO> dias = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate dia = hoje.plusDays(i);
            ServicoFuncionamento.Janela janela = servicoFuncionamento.janelaDa(empresa, dia);
            boolean disponivel = servicoFuncionamento.situacaoDoDia(empresa, dia, janela, ocupados)
                    == ServicoFuncionamento.Situacao.LIVRE;
            dias.add(new DiaReservaDTO(dia.format(formatadorDia),
                    dia.getDayOfWeek().getDisplayName(TextStyle.FULL, idiomaBrasil),
                    disponivel ? "Disponível" : "Alugado"));
        }
        return dias;
    }
}
