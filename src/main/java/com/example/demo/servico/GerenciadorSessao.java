package com.example.demo.servico;

import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.DadosReuniaoSessao;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.util.WhatsappUtil;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GerenciadorSessao {

    public static final int TTL_MINUTOS = 30;
    public static final int TRAVA_PROPRIETARIO_MINUTOS = 20;
    private static final String SESSAO_PADRAO = "_global";

    public record SessaoAtendimento(String estadoAtual, LocalDateTime ultimaInteracao) {
    }

    public record DataPendente(LocalDate data, EstadoUsuario retorno) {
    }

    private final Clock clock;
    private final Map<String, SessaoAtendimento> sessoes = new ConcurrentHashMap<>();
    private final Map<String, DadosReservaSessao> reservas = new ConcurrentHashMap<>();
    private final Map<String, DadosReuniaoSessao> reunioes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> travasProprietario = new ConcurrentHashMap<>();
    private final Map<String, String> apelidos = new ConcurrentHashMap<>();
    private final Map<String, DataPendente> datasPendentes = new ConcurrentHashMap<>();

    public GerenciadorSessao() {
        this(Clock.systemDefaultZone());
    }

    GerenciadorSessao(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public String obterEstado(String numeroCliente) {
        return obterEstado(SESSAO_PADRAO, numeroCliente);
    }

    public String obterEstado(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return EstadoUsuario.INICIO.name();
        }

        String chaveSessao = chave(sessaoWhatsapp, numeroCliente);
        LocalDateTime agora = LocalDateTime.now(clock);

        SessaoAtendimento sessao = sessoes.compute(chaveSessao, (ignorado, atual) -> {
            if (atual == null) {
                return null;
            }
            if (expirou(atual, agora)) {
                reservas.remove(chaveSessao);
                reunioes.remove(chaveSessao);
                datasPendentes.remove(chaveSessao);
                return null;
            }
            return atual;
        });

        return sessao == null || sessao.estadoAtual() == null || sessao.estadoAtual().isBlank()
                ? EstadoUsuario.INICIO.name()
                : sessao.estadoAtual();
    }

    public void atualizarEstado(String numeroCliente, String novoEstado) {
        atualizarEstado(SESSAO_PADRAO, numeroCliente, novoEstado);
    }

    public void atualizarEstado(String sessaoWhatsapp, String numeroCliente, String novoEstado) {
        if (numeroCliente == null || numeroCliente.isBlank() || novoEstado == null || novoEstado.isBlank()) {
            return;
        }

        String chaveSessao = chave(sessaoWhatsapp, numeroCliente);
        sessoes.put(chaveSessao, new SessaoAtendimento(novoEstado, LocalDateTime.now(clock)));
        if (EstadoUsuario.INICIO.name().equals(novoEstado)) {
            reservas.remove(chaveSessao);
            reunioes.remove(chaveSessao);
            datasPendentes.remove(chaveSessao);
        }
    }

    public void guardarDataPendente(String numeroCliente, LocalDate data, EstadoUsuario retorno) {
        guardarDataPendente(SESSAO_PADRAO, numeroCliente, data, retorno);
    }

    public void guardarDataPendente(String sessaoWhatsapp, String numeroCliente, LocalDate data, EstadoUsuario retorno) {
        if (numeroCliente == null || numeroCliente.isBlank() || data == null || retorno == null) {
            return;
        }
        datasPendentes.put(chave(sessaoWhatsapp, numeroCliente), new DataPendente(data, retorno));
    }

    public DataPendente obterDataPendente(String numeroCliente) {
        return obterDataPendente(SESSAO_PADRAO, numeroCliente);
    }

    public DataPendente obterDataPendente(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return null;
        }
        return datasPendentes.get(chave(sessaoWhatsapp, numeroCliente));
    }

    public void limparDataPendente(String numeroCliente) {
        limparDataPendente(SESSAO_PADRAO, numeroCliente);
    }

    public void limparDataPendente(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return;
        }
        datasPendentes.remove(chave(sessaoWhatsapp, numeroCliente));
    }

    public void concluirRegistro(String numeroCliente, EstadoUsuario novoEstado) {
        concluirRegistro(SESSAO_PADRAO, numeroCliente, novoEstado);
    }

    public void concluirRegistro(String sessaoWhatsapp, String numeroCliente, EstadoUsuario novoEstado) {
        if (numeroCliente == null || numeroCliente.isBlank() || novoEstado == null) {
            return;
        }

        String chaveSessao = chave(sessaoWhatsapp, numeroCliente);
        boolean[] trocou = {false};
        sessoes.computeIfPresent(chaveSessao, (ignorado, atual) -> {
            if (!EstadoUsuario.RESERVA_REGISTRANDO.name().equals(atual.estadoAtual())) {
                return atual;
            }
            trocou[0] = true;
            return new SessaoAtendimento(novoEstado.name(), LocalDateTime.now(clock));
        });
        if (trocou[0] && novoEstado == EstadoUsuario.INICIO) {
            reservas.remove(chaveSessao);
        }
    }

    public EstadoUsuario getEstado(String remetente) {
        return getEstado(SESSAO_PADRAO, remetente);
    }

    public EstadoUsuario getEstado(String sessaoWhatsapp, String remetente) {
        return paraEnum(obterEstado(sessaoWhatsapp, remetente));
    }

    public void setEstado(String remetente, EstadoUsuario estado) {
        setEstado(SESSAO_PADRAO, remetente, estado);
    }

    public void setEstado(String sessaoWhatsapp, String remetente, EstadoUsuario estado) {
        if (estado == null) {
            return;
        }
        atualizarEstado(sessaoWhatsapp, remetente, estado.name());
    }

    public void ativarTravaProprietario(String numeroCliente) {
        ativarTravaProprietario(SESSAO_PADRAO, numeroCliente);
    }

    public void ativarTravaProprietario(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return;
        }
        String chaveSessao = chave(sessaoWhatsapp, numeroCliente);
        LocalDateTime agora = LocalDateTime.now(clock);
        travasProprietario.put(chaveSessao, agora);
        String outroContato = apelidos.get(chaveSessao);
        if (outroContato != null) {
            travasProprietario.put(chave(sessaoWhatsapp, outroContato), agora);
        }
    }

    public void registrarApelido(String identificador, String outroIdentificador) {
        registrarApelido(SESSAO_PADRAO, identificador, outroIdentificador);
    }

    public void registrarApelido(String sessaoWhatsapp, String identificador, String outroIdentificador) {
        if (identificador == null || identificador.isBlank() || outroIdentificador == null || outroIdentificador.isBlank()) {
            return;
        }
        String normA = WhatsappUtil.normalizarParaChaveEstado(identificador);
        String normB = WhatsappUtil.normalizarParaChaveEstado(outroIdentificador);
        if (normA.isEmpty() || normB.isEmpty() || normA.equals(normB)) {
            return;
        }
        apelidos.put(chave(sessaoWhatsapp, normA), normB);
        apelidos.put(chave(sessaoWhatsapp, normB), normA);
    }

    public String apelidoDe(String numeroCliente) {
        return apelidoDe(SESSAO_PADRAO, numeroCliente);
    }

    public String apelidoDe(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return null;
        }
        return apelidos.get(chave(sessaoWhatsapp, numeroCliente));
    }

    public boolean estaComTravaProprietarioAtiva(String numeroCliente) {
        return estaComTravaProprietarioAtiva(SESSAO_PADRAO, numeroCliente);
    }

    public boolean estaComTravaProprietarioAtiva(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return false;
        }
        String chaveSessao = chave(sessaoWhatsapp, numeroCliente);
        LocalDateTime agora = LocalDateTime.now(clock);
        if (travaVigente(chaveSessao, agora)) {
            return true;
        }
        String outroContato = apelidos.get(chaveSessao);
        return outroContato != null && travaVigente(chave(sessaoWhatsapp, outroContato), agora);
    }

    private boolean travaVigente(String chaveSessao, LocalDateTime agora) {
        LocalDateTime momento = travasProprietario.get(chaveSessao);
        if (momento == null) {
            return false;
        }
        if (Duration.between(momento, agora).toMinutes() >= TRAVA_PROPRIETARIO_MINUTOS) {
            travasProprietario.remove(chaveSessao);
            return false;
        }
        return true;
    }

    public void removerTravaProprietario(String numeroCliente) {
        removerTravaProprietario(SESSAO_PADRAO, numeroCliente);
    }

    public void removerTravaProprietario(String sessaoWhatsapp, String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return;
        }
        String chaveSessao = chave(sessaoWhatsapp, numeroCliente);
        travasProprietario.remove(chaveSessao);
        String outroContato = apelidos.get(chaveSessao);
        if (outroContato != null) {
            travasProprietario.remove(chave(sessaoWhatsapp, outroContato));
        }
    }

    public DadosReservaSessao getOuCriarReserva(String remetente) {
        return getOuCriarReserva(SESSAO_PADRAO, remetente);
    }

    public DadosReservaSessao getOuCriarReserva(String sessaoWhatsapp, String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return new DadosReservaSessao();
        }
        return reservas.computeIfAbsent(chave(sessaoWhatsapp, remetente), ignorado -> new DadosReservaSessao());
    }

    public DadosReuniaoSessao getOuCriarReuniao(String remetente) {
        return getOuCriarReuniao(SESSAO_PADRAO, remetente);
    }

    public DadosReuniaoSessao getOuCriarReuniao(String sessaoWhatsapp, String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return new DadosReuniaoSessao();
        }
        return reunioes.computeIfAbsent(chave(sessaoWhatsapp, remetente), ignorado -> new DadosReuniaoSessao());
    }

    public void limparReuniao(String remetente) {
        limparReuniao(SESSAO_PADRAO, remetente);
    }

    public void limparReuniao(String sessaoWhatsapp, String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return;
        }
        reunioes.remove(chave(sessaoWhatsapp, remetente));
    }

    public DadosReservaSessao getReserva(String remetente) {
        return getReserva(SESSAO_PADRAO, remetente);
    }

    public DadosReservaSessao getReserva(String sessaoWhatsapp, String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return null;
        }
        return reservas.get(chave(sessaoWhatsapp, remetente));
    }

    public void limparReserva(String remetente) {
        limparReserva(SESSAO_PADRAO, remetente);
    }

    public void limparReserva(String sessaoWhatsapp, String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return;
        }
        reservas.remove(chave(sessaoWhatsapp, remetente));
    }

    private boolean expirou(SessaoAtendimento sessao, LocalDateTime agora) {
        if (sessao.ultimaInteracao() == null) {
            return true;
        }
        return Duration.between(sessao.ultimaInteracao(), agora).toMinutes() > TTL_MINUTOS;
    }

    private EstadoUsuario paraEnum(String estado) {
        if (estado == null || estado.isBlank()) {
            return EstadoUsuario.INICIO;
        }
        try {
            return EstadoUsuario.valueOf(estado);
        } catch (IllegalArgumentException e) {
            return EstadoUsuario.INICIO;
        }
    }

    private String chave(String sessaoWhatsapp, String remetente) {
        String contato = WhatsappUtil.normalizarParaChaveEstado(remetente);
        if (contato.isEmpty()) {
            return "";
        }
        String sessaoNorm = (sessaoWhatsapp == null || sessaoWhatsapp.isBlank())
                ? SESSAO_PADRAO
                : sessaoWhatsapp.trim();
        return sessaoNorm + "|" + contato;
    }
}
