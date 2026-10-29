package com.example.demo.servico;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ServicoGoogleagenda {

    private static final Logger log = LoggerFactory.getLogger(ServicoGoogleagenda.class);
    private static final String ARQUIVO_CREDENCIAL = "/credencial.json";

    private Calendar servicoGoogleEmMemoria;

    @PostConstruct
    public void inicializarAgenda() {
        try (InputStream in = ServicoGoogleagenda.class.getResourceAsStream(ARQUIVO_CREDENCIAL)) {
            if (in == null) {
                log.warn("Arquivo {} não encontrado em resources. Google Agenda permanece desligado; a API sobe normalmente.",
                        ARQUIVO_CREDENCIAL);
                return;
            }

            log.info("Inicializando conexão com Google Agenda...");
            GoogleCredential credential = GoogleCredential.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            this.servicoGoogleEmMemoria = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Bot Reservas Area de Lazer")
                    .build();

            log.info("Google Agenda inicializado e salvo em memória.");
        } catch (FileNotFoundException e) {
            log.warn("Arquivo {} não encontrado. Google Agenda permanece desligado. {}", ARQUIVO_CREDENCIAL, e.getClass().getSimpleName());
        } catch (IOException | RuntimeException e) {
            log.warn("Não foi possível inicializar o Google Agenda. A API sobe normalmente. Motivo: {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("Falha inesperada ao conectar com Google Agenda. A API sobe normalmente. Motivo: {}", e.getClass().getSimpleName());
        }
    }

    public Calendar conectarAgenda() {
        if (this.servicoGoogleEmMemoria == null) {
            throw new IllegalStateException("Google Agenda não está inicializado (credencial ausente ou inválida).");
        }
        return this.servicoGoogleEmMemoria;
    }

    public boolean dataEstaLivre(String calendarId, LocalDate data) throws IOException {
        return datasOcupadas(calendarId, data, data.plusDays(1)).isEmpty();
    }

    public Set<LocalDate> datasOcupadas(String calendarId, LocalDate inicio, LocalDate fimExclusivo) throws IOException {
        Calendar servico = conectarAgenda();
        ZoneId fusoBrasilia = ZoneId.of("America/Sao_Paulo");
        DateTime tempoMinimo = new DateTime(inicio.atStartOfDay(fusoBrasilia).toInstant().toEpochMilli());
        DateTime tempoMaximo = new DateTime(fimExclusivo.atStartOfDay(fusoBrasilia).toInstant().toEpochMilli());

        Events eventos = servico.events().list(calendarId)
                .setTimeMin(tempoMinimo)
                .setTimeMax(tempoMaximo)
                .setSingleEvents(true)
                .setMaxResults(250)
                .execute();

        Set<LocalDate> ocupadas = new HashSet<>();
        List<Event> itens = eventos.getItems();
        if (itens == null) {
            return ocupadas;
        }
        for (Event evento : itens) {
            LocalDate dia = extrairData(evento.getStart());
            if (dia != null) {
                ocupadas.add(dia);
            }
        }
        return ocupadas;
    }

    public record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
    }

    public static boolean conflita(List<Intervalo> ocupados, LocalDateTime inicio, LocalDateTime fim, int intervaloMinutos) {
        LocalDateTime inicioComFolga = inicio.minusMinutes(intervaloMinutos);
        LocalDateTime fimComFolga = fim.plusMinutes(intervaloMinutos);
        for (Intervalo ocupado : ocupados) {
            if (ocupado.inicio().isBefore(fimComFolga) && ocupado.fim().isAfter(inicioComFolga)) {
                return true;
            }
        }
        return false;
    }

    public List<Intervalo> ocupacoesDoDia(String calendarId, LocalDate data) throws IOException {
        return ocupacoesDoPeriodo(calendarId, data, data.plusDays(1));
    }

    public List<Intervalo> ocupacoesDoPeriodo(String calendarId, LocalDate inicio, LocalDate fimExclusivo) throws IOException {
        Calendar servico = conectarAgenda();
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        DateTime tempoMinimo = new DateTime(inicio.atStartOfDay(fuso).toInstant().toEpochMilli());
        DateTime tempoMaximo = new DateTime(fimExclusivo.atStartOfDay(fuso).toInstant().toEpochMilli());

        Events eventos = servico.events().list(calendarId)
                .setTimeMin(tempoMinimo)
                .setTimeMax(tempoMaximo)
                .setSingleEvents(true)
                .setMaxResults(500)
                .execute();

        List<Intervalo> ocupados = new ArrayList<>();
        List<Event> itens = eventos.getItems();
        if (itens == null) {
            return ocupados;
        }
        for (Event evento : itens) {
            if ("cancelled".equals(evento.getStatus())) {
                continue;
            }
            Intervalo intervalo = paraIntervalo(evento, fuso);
            if (intervalo != null) {
                ocupados.add(intervalo);
            }
        }
        return ocupados;
    }

    public List<String> resumosDoDia(String calendarId, LocalDate data) throws IOException {
        Calendar servico = conectarAgenda();
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Events eventos = servico.events().list(calendarId)
                .setTimeMin(new DateTime(data.atStartOfDay(fuso).toInstant().toEpochMilli()))
                .setTimeMax(new DateTime(data.plusDays(1).atStartOfDay(fuso).toInstant().toEpochMilli()))
                .setSingleEvents(true)
                .setMaxResults(50)
                .execute();
        List<String> resumos = new ArrayList<>();
        if (eventos.getItems() == null) {
            return resumos;
        }
        for (Event evento : eventos.getItems()) {
            if ("cancelled".equals(evento.getStatus())) {
                continue;
            }
            Intervalo intervalo = paraIntervalo(evento, fuso);
            String titulo = evento.getSummary() == null ? "Evento" : evento.getSummary();
            if (intervalo == null || evento.getStart().getDate() != null) {
                resumos.add("Dia inteiro - " + titulo);
            } else {
                resumos.add(intervalo.inicio().toLocalTime() + " às " + intervalo.fim().toLocalTime() + " - " + titulo);
            }
        }
        return resumos;
    }

    public boolean horarioEstaLivre(String calendarId, LocalDate data, LocalTime inicio, LocalTime fim, int intervaloMinutos)
            throws IOException {
        return !conflita(ocupacoesDoDia(calendarId, data), data.atTime(inicio), data.atTime(fim), intervaloMinutos);
    }

    public List<LocalTime> horariosLivres(String calendarId, LocalDate data, List<LocalTime> iniciosPossiveis,
                                          int duracaoMinutos, int intervaloMinutos, LocalTime aPartirDe) throws IOException {
        List<Intervalo> ocupados = ocupacoesDoDia(calendarId, data);
        List<LocalTime> livres = new ArrayList<>();
        for (LocalTime inicio : iniciosPossiveis) {
            if (aPartirDe != null && inicio.isBefore(aPartirDe)) {
                continue;
            }
            LocalDateTime comeco = data.atTime(inicio);
            if (!conflita(ocupados, comeco, comeco.plusMinutes(duracaoMinutos), intervaloMinutos)) {
                livres.add(inicio);
            }
        }
        return livres;
    }

    private Intervalo paraIntervalo(Event evento, ZoneId fuso) {
        EventDateTime inicio = evento.getStart();
        EventDateTime fim = evento.getEnd();
        if (inicio == null) {
            return null;
        }
        if (inicio.getDate() != null) {
            LocalDate dia = LocalDate.parse(inicio.getDate().toStringRfc3339().substring(0, 10));
            LocalDate ultimo = fim != null && fim.getDate() != null
                    ? LocalDate.parse(fim.getDate().toStringRfc3339().substring(0, 10))
                    : dia.plusDays(1);
            return new Intervalo(dia.atStartOfDay(), ultimo.atStartOfDay());
        }
        if (inicio.getDateTime() == null) {
            return null;
        }
        LocalDateTime comeco = Instant.ofEpochMilli(inicio.getDateTime().getValue()).atZone(fuso).toLocalDateTime();
        LocalDateTime termino = fim != null && fim.getDateTime() != null
                ? Instant.ofEpochMilli(fim.getDateTime().getValue()).atZone(fuso).toLocalDateTime()
                : comeco.plusHours(1);
        return new Intervalo(comeco, termino);
    }

    private LocalDate extrairData(EventDateTime inicio) {
        if (inicio == null) {
            return null;
        }
        if (inicio.getDate() != null) {
            return LocalDate.parse(inicio.getDate().toStringRfc3339().substring(0, 10));
        }
        if (inicio.getDateTime() != null) {
            return Instant.ofEpochMilli(inicio.getDateTime().getValue())
                    .atZone(ZoneId.of("America/Sao_Paulo"))
                    .toLocalDate();
        }
        return null;
    }
}
