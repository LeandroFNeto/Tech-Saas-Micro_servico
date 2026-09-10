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
import java.time.ZoneId;
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
            log.warn("Arquivo {} não encontrado. Google Agenda permanece desligado. {}", ARQUIVO_CREDENCIAL, e.getMessage());
        } catch (IOException | RuntimeException e) {
            log.warn("Não foi possível inicializar o Google Agenda. A API sobe normalmente. Motivo: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Falha inesperada ao conectar com Google Agenda. A API sobe normalmente. Motivo: {}", e.getMessage());
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
