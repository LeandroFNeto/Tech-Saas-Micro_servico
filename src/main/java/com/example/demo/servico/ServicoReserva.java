package com.example.demo.servico;

import com.example.demo.dto.DiaReservaDTO;
import com.example.demo.model.Empresa;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.format.TextStyle;

@Service
public class ServicoReserva {

    @Autowired
    private ServicoGoogleagenda agendaService;

    // 1. Método para CRIAR a reserva (Já estava quase certo)
    public void agendarNoGoogle(Empresa empresa, String dataReserva, String nomeCliente) throws Exception {
        com.google.api.services.calendar.Calendar servicoAgenda = agendaService.conectarAgenda();
        String calendarId = empresa.getGoogleCalendarId();

        if(calendarId == null || calendarId.isEmpty()) {
            System.out.println("⚠️ A empresa " + empresa.getNome() + " não cadastrou o ID da agenda no banco!");
            return;
        }


        System.out.println("✅ Reserva salva na agenda da empresa: " + empresa.getNome());
    }

    // 2. Método para LISTAR (Agora recebe a Empresa)
    // Removi o @GetMapping temporariamente se este for chamado diretamente pelo ServicoLocacao
    public List<DiaReservaDTO> listarReservas(Empresa empresa){
        try{
            com.google.api.services.calendar.Calendar servicoGoogle = agendaService.conectarAgenda();
            DateTime agora = new DateTime(System.currentTimeMillis());

            String calendarId = empresa.getGoogleCalendarId();
            if(calendarId == null) return null; // Trava de segurança

            // CORREÇÃO: Passamos a variável calendarId no .list()
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
            System.out.println("⚠️ ERRO CRÍTICO NO GOOGLE CALENDAR (Listar): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 3. Método para PESQUISAR (Agora recebe a Empresa também)
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

            // CORREÇÃO: Passamos a variável calendarId no .list()
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
            System.out.println("⚠️ ERRO CRÍTICO NO GOOGLE CALENDAR (Pesquisar): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao buscar data: " + e.getMessage());
        }
    }
}