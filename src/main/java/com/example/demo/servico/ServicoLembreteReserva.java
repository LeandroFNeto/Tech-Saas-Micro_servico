package com.example.demo.servico;

import com.example.demo.model.*;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ServicoLembreteReserva {

    private static final Logger log = LoggerFactory.getLogger(ServicoLembreteReserva.class);
    private static final String TEXTO_PADRAO = "Olá, {nome}! Tudo bem? Gostaria de confirmar sua reserva para {data} às {horario}. Podemos confirmar?";
    private static final int LIMITE_DIARIO_ENVIOS = 50;
    private static final long INTERVALO_MINIMO_MS = 5000;

    private final Map<String, AtomicInteger> enviosPorEmpresaEDia = new ConcurrentHashMap<>();
    private final Map<Long, Long> ultimoEnvioMillisPorEmpresa = new ConcurrentHashMap<>();

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private AuditoriaAcessoContatoRepository auditoriaRepository;

    @Autowired
    private ServicoMensagem servicoMensagem;

    @Value("${retencao.meses:12}")
    private int mesesRetencao;

    @Scheduled(cron = "0 */15 * * * *", zone = "America/Sao_Paulo")
    public void rotinaAgendada() {
        executarCiclo(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
    }

    @Transactional
    public void executarCiclo(LocalDateTime agora) {
        if (!CriptografiaCampo.isConfigurado()) {
            CriptografiaCampo.avisarChavesNaoConfiguradasSeNecessario();
            return;
        }

        LocalDate hoje = agora.toLocalDate();
        List<Reserva> passadas = reservaRepository.findByDataLessThanAndSituacaoNot(hoje, SituacaoReserva.ENCERRADA);
        for (Reserva r : passadas) {
            r.setSituacao(SituacaoReserva.ENCERRADA);
            r.setAtualizadaEm(agora);
            reservaRepository.save(r);
        }

        List<Reserva> enviadas = reservaRepository.findBySituacaoAndLembreteEnviadoEmLessThanEqual(
                SituacaoReserva.LEMBRETE_ENVIADO, agora.minusHours(12));
        for (Reserva r : enviadas) {
            r.setSituacao(SituacaoReserva.SEM_RESPOSTA);
            r.setAtualizadaEm(agora);
            reservaRepository.save(r);
        }

        List<Reserva> presas = reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(
                SituacaoReserva.ENVIANDO, agora.minusMinutes(30));
        for (Reserva r : presas) {
            r.setSituacao(SituacaoReserva.FALHA_ENVIO);
            r.setAtualizadaEm(agora);
            reservaRepository.save(r);
        }

        List<Reserva> aguardando = reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(
                SituacaoReserva.AGUARDANDO_LEMBRETE, agora);
        for (Reserva r : aguardando) {
            tratarReservaAguardando(r, agora);
        }

        List<Reserva> falhas = reservaRepository.findBySituacaoAndLembreteEnviarEmLessThanEqual(
                SituacaoReserva.FALHA_ENVIO, agora);
        for (Reserva r : falhas) {
            if (r.getTentativasEnvio() < 3 && r.getEmpresa().getLembreteModo() == ModoLembrete.AUTOMATICO) {
                tentarEnvioAutomatico(r, agora);
            }
        }
    }

    private void tratarReservaAguardando(Reserva r, LocalDateTime agora) {
        if (r.getContato() == null || r.getContato().isNaoContatar()) {
            return;
        }
        ModoLembrete modo = r.getEmpresa().getLembreteModo();
        if (modo == ModoLembrete.SUGERIDO) {
            r.setSituacao(SituacaoReserva.PRONTA_PARA_AVISAR);
            r.setAtualizadaEm(agora);
            reservaRepository.save(r);
        } else if (modo == ModoLembrete.AUTOMATICO) {
            tentarEnvioAutomatico(r, agora);
        }
    }

    private void tentarEnvioAutomatico(Reserva r, LocalDateTime agora) {
        LocalTime hora = agora.toLocalTime();
        if (hora.isAfter(LocalTime.of(20, 0)) || hora.isBefore(LocalTime.of(8, 0))) {
            return;
        }
        enviarLembrete(r, agora);
    }

    @Transactional
    public boolean enviarLembrete(Reserva reserva, LocalDateTime agora) {
        if (reserva.getContato() == null || reserva.getContato().isNaoContatar()) {
            return false;
        }
        LocalDate hoje = agora.toLocalDate();
        if (reserva.getData().isBefore(hoje)) {
            return false;
        }

        Empresa empresa = reserva.getEmpresa();
        String chaveDia = empresa.getId() + "_" + hoje;
        AtomicInteger contador = enviosPorEmpresaEDia.computeIfAbsent(chaveDia, k -> new AtomicInteger(0));
        if (contador.get() >= LIMITE_DIARIO_ENVIOS) {
            log.warn("Limite diario de 50 mensagens excedido para a empresa {}", empresa.getId());
            return false;
        }

        long agoraMillis = System.currentTimeMillis();
        Long ultimo = ultimoEnvioMillisPorEmpresa.get(empresa.getId());
        if (ultimo != null && (agoraMillis - ultimo) < INTERVALO_MINIMO_MS) {
            try {
                Thread.sleep(INTERVALO_MINIMO_MS - (agoraMillis - ultimo));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        reserva.setSituacao(SituacaoReserva.ENVIANDO);
        reserva.setTentativasEnvio(reserva.getTentativasEnvio() + 1);
        reserva.setAtualizadaEm(agora);
        reservaRepository.save(reserva);

        String textoTemplate = empresa.getLembreteTexto();
        if (textoTemplate == null || textoTemplate.isBlank()) {
            textoTemplate = TEXTO_PADRAO;
        }

        String nome = (reserva.getContato().getNome() != null && !reserva.getContato().getNome().isBlank())
                ? reserva.getContato().getNome()
                : (reserva.getTitulo() != null && reserva.getTitulo().startsWith("Reserva - ")
                ? reserva.getTitulo().substring("Reserva - ".length())
                : "");

        String dataStr = reserva.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horarioStr = reserva.getHorario() != null ? reserva.getHorario() : "Diária Completa";

        String textoFinal = textoTemplate
                .replace("{nome}", nome)
                .replace("{data}", dataStr)
                .replace("{horario}", horarioStr);

        boolean sucesso = servicoMensagem.tentarEnviarMensagemWPP(
                empresa,
                reserva.getContato().getWhatsappId(),
                textoFinal);

        ultimoEnvioMillisPorEmpresa.put(empresa.getId(), System.currentTimeMillis());

        if (sucesso) {
            contador.incrementAndGet();
            reserva.setSituacao(SituacaoReserva.LEMBRETE_ENVIADO);
            reserva.setLembreteEnviadoEm(agora);
            reserva.setAtualizadaEm(agora);
            reservaRepository.save(reserva);
            return true;
        } else {
            reserva.setSituacao(SituacaoReserva.FALHA_ENVIO);
            reserva.setAtualizadaEm(agora);
            reservaRepository.save(reserva);
            return false;
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void rotinaRetencaoDiaria() {
        executarRetencao(LocalDate.now(ZoneId.of("America/Sao_Paulo")), mesesRetencao);
    }

    @Transactional
    public void executarRetencao(LocalDate hoje, int meses) {
        LocalDate limiteData = hoje.minusMonths(meses);
        LocalDateTime limiteDataHora = limiteData.atStartOfDay();

        reservaRepository.apagarReservasEncerradasAntesDe(limiteData);
        auditoriaRepository.apagarAuditoriasAntesDe(limiteDataHora);

        List<Contato> inativos = contatoRepository.findInativosParaRetencao(limiteDataHora, limiteData);
        if (!inativos.isEmpty()) {
            contatoRepository.deleteAll(inativos);
        }
    }

    public void zerarContadoresParaTestes() {
        enviosPorEmpresaEDia.clear();
        ultimoEnvioMillisPorEmpresa.clear();
    }
}
