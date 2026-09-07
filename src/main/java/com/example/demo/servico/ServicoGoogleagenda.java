package com.example.demo.servico;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Service
public class ServicoGoogleagenda {

    private static final Logger log = LoggerFactory.getLogger(ServicoGoogleagenda.class);
    private static final String ARQUIVO_CREDENCIAL = "/crendecial.json";

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
}
