package com.example.demo.servico;

import com.example.demo.model.*;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ServicoSincronizacaoAgenda {

    private static final Logger log = LoggerFactory.getLogger(ServicoSincronizacaoAgenda.class);
    private static final Pattern PADRAO_TELEFONE_BR = Pattern.compile(
            "(?:\\+?55\\s*)?(?:\\(?([1-9][0-9])\\)?\\s*)(?:(9[0-9]{4}|[2-9][0-9]{3})[-.\\s]?([0-9]{4}))\\b"
    );

    private final Map<Long, LocalDateTime> ultimaSincronizacaoPorEmpresa = new ConcurrentHashMap<>();

    @Autowired
    private ServicoReserva servicoReserva;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void sincronizarDeHoraEmHora() {
        List<Empresa> empresas = empresaRepository.findAll();
        for (Empresa empresa : empresas) {
            if (empresa.getGoogleCalendarId() != null && !empresa.getGoogleCalendarId().isBlank()
                    && empresa.getLembreteModo() != ModoLembrete.DESLIGADO) {
                try {
                    sincronizarAgenda(empresa, 30, true);
                } catch (Exception e) {
                    log.error("Erro na sincronizacao horaria da agenda (empresa {}): {}",
                            empresa.getId(), e.getClass().getSimpleName());
                }
            }
        }
    }

    public boolean sincronizarSeNecessario(Empresa empresa, int dias) {
        LocalDateTime ultima = ultimaSincronizacaoPorEmpresa.get(empresa.getId());
        LocalDateTime agora = LocalDateTime.now();
        if (ultima != null && agora.isBefore(ultima.plusMinutes(5))) {
            return true;
        }
        return sincronizarAgenda(empresa, dias, false);
    }

    @Transactional
    public boolean sincronizarAgenda(Empresa empresa, int dias, boolean forcado) {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            return true;
        }
        if (!CriptografiaCampo.isConfigurado()) {
            CriptografiaCampo.avisarChavesNaoConfiguradasSeNecessario();
            return false;
        }

        List<ServicoReserva.EventoAgendaDTO> eventos;
        try {
            eventos = servicoReserva.listarEventosProximos(empresa, dias);
        } catch (Exception e) {
            log.error("Falha ao consultar Google Agenda para sincronizacao (empresa {}): {}",
                    empresa.getId(), e.getClass().getSimpleName());
            return false;
        }

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(fuso);
        LocalDate fim = hoje.plusDays(dias);

        Set<String> idsEventosAtivos = new HashSet<>();

        for (ServicoReserva.EventoAgendaDTO evento : eventos) {
            if ("cancelled".equalsIgnoreCase(evento.status())) {
                tratarEventoCancelado(empresa, evento.id());
                continue;
            }

            idsEventosAtivos.add(evento.id());
            upsertReserva(empresa, evento);
        }

        List<Reserva> locais = reservaRepository.findByEmpresaIdAndDataBetweenOrderByDataAscHorarioAsc(
                empresa.getId(), hoje, fim);
        for (Reserva local : locais) {
            if (local.getGoogleEventId() != null
                    && !idsEventosAtivos.contains(local.getGoogleEventId())
                    && local.getData().isAfter(hoje.minusDays(1))
                    && local.getSituacao() != SituacaoReserva.CANCELADA_NA_AGENDA
                    && local.getSituacao() != SituacaoReserva.ENCERRADA) {
                local.setSituacao(SituacaoReserva.CANCELADA_NA_AGENDA);
                local.setAtualizadaEm(LocalDateTime.now());
                reservaRepository.save(local);
            }
        }

        ultimaSincronizacaoPorEmpresa.put(empresa.getId(), LocalDateTime.now());
        return true;
    }

    private void tratarEventoCancelado(Empresa empresa, String googleEventId) {
        reservaRepository.findByEmpresaIdAndGoogleEventId(empresa.getId(), googleEventId)
                .ifPresent(reserva -> {
                    LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
                    if (!reserva.getData().isBefore(hoje)
                            && reserva.getSituacao() != SituacaoReserva.CANCELADA_NA_AGENDA
                            && reserva.getSituacao() != SituacaoReserva.ENCERRADA) {
                        reserva.setSituacao(SituacaoReserva.CANCELADA_NA_AGENDA);
                        reserva.setAtualizadaEm(LocalDateTime.now());
                        reservaRepository.save(reserva);
                    }
                });
    }

    private void upsertReserva(Empresa empresa, ServicoReserva.EventoAgendaDTO evento) {
        Reserva reserva = reservaRepository.findByEmpresaIdAndGoogleEventId(empresa.getId(), evento.id())
                .orElse(null);

        boolean nova = (reserva == null);
        if (nova) {
            reserva = new Reserva();
            reserva.setEmpresa(empresa);
            reserva.setGoogleEventId(evento.id());
            reserva.setOrigem(OrigemReserva.AGENDA);
        }

        boolean mudouDataOuHorario = !nova && (!Objects.equals(reserva.getData(), evento.data())
                || !Objects.equals(reserva.getHorario(), evento.horario()));

        reserva.setData(evento.data());
        reserva.setHorario(evento.horario());
        reserva.setTitulo(evento.titulo() != null ? evento.titulo() : "");
        reserva.setAtualizadaEm(LocalDateTime.now());

        if (nova || reserva.getContato() == null) {
            String textoBusca = (evento.titulo() != null ? evento.titulo() : "") + " "
                    + (evento.descricao() != null ? evento.descricao() : "");
            String telefoneBr = extrairTelefoneBrasileiro(textoBusca);

            if (telefoneBr != null) {
                Contato contato = obterOuCriarContatoPorTelefone(empresa, telefoneBr);
                reserva.setContato(contato);
                if (nova) {
                    reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);
                }
            } else {
                if (nova) {
                    reserva.setSituacao(SituacaoReserva.SEM_CONTATO);
                }
            }
        }

        if (nova || mudouDataOuHorario) {
            if (reserva.getSituacao() == SituacaoReserva.AGUARDANDO_LEMBRETE
                    || reserva.getSituacao() == SituacaoReserva.PRONTA_PARA_AVISAR) {
                reserva.setLembreteEnviarEm(calcularLembreteEnviarEm(empresa, evento.data()));
            }
        }

        reservaRepository.save(reserva);
    }

    public Contato obterOuCriarContatoPorTelefone(Empresa empresa, String telefoneBr) {
        String whatsappId = telefoneBr + "@c.us";
        String numeroChave = WhatsappUtil.normalizarParaChaveEstado(whatsappId);
        String numeroIndice = CriptografiaCampo.calcularIndice(numeroChave);
        String finalTelefone = telefoneBr.substring(telefoneBr.length() - 4);

        return contatoRepository.findByEmpresaIdAndNumeroIndice(empresa.getId(), numeroIndice)
                .orElseGet(() -> {
                    Contato novo = new Contato();
                    novo.setEmpresa(empresa);
                    novo.setWhatsappId(whatsappId);
                    novo.setNumeroIndice(numeroIndice);
                    novo.setFinalTelefone(finalTelefone);
                    novo.setPrimeiraConversaEm(null);
                    novo.setUltimaConversaEm(null);
                    novo.setNaoContatar(false);
                    novo.setOrigem(OrigemContato.AGENDA);
                    return contatoRepository.save(novo);
                });
    }

    public static String extrairTelefoneBrasileiro(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        Matcher matcher = PADRAO_TELEFONE_BR.matcher(texto);
        if (matcher.find()) {
            String ddd = matcher.group(1);
            String parte1 = matcher.group(2).replaceAll("\\D", "");
            String parte2 = matcher.group(3).replaceAll("\\D", "");
            return "55" + ddd + parte1 + parte2;
        }
        return null;
    }

    public static LocalDateTime calcularLembreteEnviarEm(Empresa empresa, LocalDate dataReserva) {
        int diasAntes = empresa.getLembreteDiasAntes();
        String horarioStr = empresa.getLembreteHorarioEnvio();
        LocalTime horario = LocalTime.parse(horarioStr);
        LocalDate diaEnvio = dataReserva.minusDays(diasAntes);
        LocalDateTime momento = diaEnvio.atTime(horario);
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fuso);
        if (momento.isBefore(agora)) {
            return null;
        }
        return momento;
    }
}
