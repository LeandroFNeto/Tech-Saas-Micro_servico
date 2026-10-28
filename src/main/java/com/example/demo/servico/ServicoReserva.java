package com.example.demo.servico;

import com.example.demo.dto.DiaReservaDTO;
import com.example.demo.model.Empresa;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.format.TextStyle;

@Service
public class ServicoReserva {

    private static final Logger log = LoggerFactory.getLogger(ServicoReserva.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String ALFABETO_BASE32HEX = "0123456789abcdefghijklmnopqrstuv";

    @Autowired
    private ServicoGoogleagenda agendaService;

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

    public boolean eventoDoPedidoExiste(Empresa empresa, String idPedido) throws Exception {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            throw new AgendaNaoConfiguradaException(empresa.getId());
        }
        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        try {
            Event existente = servicoAgenda.events().get(calendarId, idEventoDoPedido(idPedido)).execute();
            return existente != null && !"cancelled".equals(existente.getStatus());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    static String idEventoDoPedido(String idPedido) {
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

    public List<DiaReservaDTO> listarReservas(Empresa empresa){
        try{
            com.google.api.services.calendar.Calendar servicoGoogle = agendaService.conectarAgenda();
            DateTime agora = new DateTime(System.currentTimeMillis());

            String calendarId = empresa.getGoogleCalendarId();
            if(calendarId == null) return null;

            Events eventos = servicoGoogle.events().list(calendarId)
                    .setMaxResults(100)
                    .setTimeMin(agora)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> itens = eventos.getItems() == null ? List.of() : eventos.getItems();

            List<DiaReservaDTO> listaDias = new ArrayList<>();
            Set<LocalDate> diasAlugados = new HashSet<>();

            for (Event evento : itens){
                var inicio = evento.getStart();
                if (inicio != null) {
                    if (inicio.getDateTime() != null){
                        ZonedDateTime dataComHora = ZonedDateTime.parse(inicio.getDateTime().toString());
                        diasAlugados.add(dataComHora.toLocalDate());
                    } else if (inicio.getDate() != null) {
                        LocalDate dataSemHora = LocalDate.parse(inicio.getDate().toString());
                        diasAlugados.add(dataSemHora);
                    }
                }
            }

            DateTimeFormatter formatadorDia = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
            Locale idiomaBrasil = new Locale("pt", "BR");

            for (int i = 0; i < 30; i++){
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

    public DiaReservaDTO pesquisarData(Empresa empresa, String dataRecebida) {
        try {
            DateTimeFormatter formatadorEntrada = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate dataAlvo = LocalDate.parse(dataRecebida, formatadorEntrada);

            if (dataAlvo.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Não é possível consultar datas que já passaram.");
            }

            ZoneId fusoBrasilia = ZoneId.of("America/Sao_Paulo");
            Locale idiomaBrasil = new Locale("pt", "BR");
            String nomeDiaSemana = dataAlvo.getDayOfWeek().getDisplayName(TextStyle.FULL, idiomaBrasil);

            ZonedDateTime inicioDoDia = dataAlvo.atStartOfDay(fusoBrasilia);
            ZonedDateTime fimDoDia = dataAlvo.atTime(23, 59, 59).atZone(fusoBrasilia);

            DateTime tempoMinimo = new DateTime(inicioDoDia.toInstant().toEpochMilli());
            DateTime tempoMaximo = new DateTime(fimDoDia.toInstant().toEpochMilli());

            com.google.api.services.calendar.Calendar servicoGoogle = agendaService.conectarAgenda();

            String calendarId = empresa.getGoogleCalendarId();
            if(calendarId == null) throw new RuntimeException("Empresa sem calendário configurado.");

            Events eventos = servicoGoogle.events().list(calendarId)
                    .setTimeMin(tempoMinimo)
                    .setTimeMax(tempoMaximo)
                    .setSingleEvents(true)
                    .execute();

            List<Event> itens = eventos.getItems();

            if (itens == null || itens.isEmpty()) {
                return new DiaReservaDTO(dataRecebida, nomeDiaSemana, "Disponível");
            } else {
                return new DiaReservaDTO(dataRecebida, nomeDiaSemana, "Alugado");
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro no Google Calendar ao pesquisar (empresa {}): {}", empresa.getId(), e.getClass().getSimpleName());
            throw new RuntimeException("Erro ao buscar data", e);
        }
    }
}