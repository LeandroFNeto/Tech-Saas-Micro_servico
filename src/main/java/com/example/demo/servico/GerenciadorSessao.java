package com.example.demo.servico;

import com.example.demo.model.DadosReservaSessao;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.util.WhatsappUtil;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GerenciadorSessao {

    public static final int TTL_MINUTOS = 30;

    public record SessaoAtendimento(String estadoAtual, LocalDateTime ultimaInteracao) {
    }

    private final Clock clock;
    private final Map<String, SessaoAtendimento> sessoes = new ConcurrentHashMap<>();
    private final Map<String, DadosReservaSessao> reservas = new ConcurrentHashMap<>();

    public GerenciadorSessao() {
        this(Clock.systemDefaultZone());
    }

    GerenciadorSessao(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public String obterEstado(String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return EstadoUsuario.INICIO.name();
        }

        String chaveSessao = chave(numeroCliente);
        LocalDateTime agora = LocalDateTime.now(clock);

        SessaoAtendimento sessao = sessoes.compute(chaveSessao, (ignorado, atual) -> {
            if (atual == null) {
                return null;
            }
            if (expirou(atual, agora)) {
                reservas.remove(chaveSessao);
                return null;
            }
            return atual;
        });

        return sessao == null || sessao.estadoAtual() == null || sessao.estadoAtual().isBlank()
                ? EstadoUsuario.INICIO.name()
                : sessao.estadoAtual();
    }

    public void atualizarEstado(String numeroCliente, String novoEstado) {
        if (numeroCliente == null || numeroCliente.isBlank() || novoEstado == null || novoEstado.isBlank()) {
            return;
        }

        String chaveSessao = chave(numeroCliente);
        sessoes.put(chaveSessao, new SessaoAtendimento(novoEstado, LocalDateTime.now(clock)));
        if (EstadoUsuario.INICIO.name().equals(novoEstado)) {
            reservas.remove(chaveSessao);
        }
    }

    public EstadoUsuario getEstado(String remetente) {
        return paraEnum(obterEstado(remetente));
    }

    public void setEstado(String remetente, EstadoUsuario estado) {
        if (estado == null) {
            return;
        }
        atualizarEstado(remetente, estado.name());
    }

    public DadosReservaSessao getOuCriarReserva(String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return new DadosReservaSessao();
        }
        return reservas.computeIfAbsent(chave(remetente), ignorado -> new DadosReservaSessao());
    }

    public DadosReservaSessao getReserva(String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return null;
        }
        return reservas.get(chave(remetente));
    }

    public void limparReserva(String remetente) {
        if (remetente == null || remetente.isBlank()) {
            return;
        }
        reservas.remove(chave(remetente));
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

    private String chave(String remetente) {
        return WhatsappUtil.normalizarParaChaveEstado(remetente);
    }
}
