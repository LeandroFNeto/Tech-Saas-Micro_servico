package com.example.demo.servico;

import com.example.demo.util.WhatsappUtil;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistroEnviosBot {

    public static final Duration VALIDADE = Duration.ofMinutes(2);
    static final int MAXIMO_POR_CONTATO = 30;
    static final int MAXIMO_CONTATOS = 5000;
    private static final String MARCA_MIDIA = "\u0000midia";

    private record Envio(String conteudo, Instant momento) {
    }

    private final Clock clock;
    private final Map<String, Deque<Envio>> envios = new ConcurrentHashMap<>();

    public RegistroEnviosBot() {
        this(Clock.systemDefaultZone());
    }

    RegistroEnviosBot(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public void registrarTexto(String contato, String texto) {
        registrar(contato, normalizar(texto));
    }

    public void registrarMidia(String contato) {
        registrar(contato, MARCA_MIDIA);
    }

    public void esquecerTexto(String contato, String texto) {
        esquecer(contato, normalizar(texto));
    }

    public void esquecerMidia(String contato) {
        esquecer(contato, MARCA_MIDIA);
    }

    private void esquecer(String contato, String conteudo) {
        String chave = chave(contato);
        Deque<Envio> fila = chave.isEmpty() ? null : envios.get(chave);
        if (fila == null || conteudo.isEmpty()) {
            return;
        }
        synchronized (fila) {
            java.util.Iterator<Envio> it = fila.descendingIterator();
            while (it.hasNext()) {
                if (it.next().conteudo().equals(conteudo)) {
                    it.remove();
                    return;
                }
            }
        }
    }

    public boolean foiEnviadoPeloBot(String contato, String texto) {
        String chave = chave(contato);
        if (chave.isEmpty()) {
            return false;
        }
        Deque<Envio> fila = envios.get(chave);
        if (fila == null) {
            return false;
        }
        String procurado = normalizar(texto);
        if (procurado.isEmpty()) {
            procurado = MARCA_MIDIA;
        }
        Instant agora = clock.instant();
        synchronized (fila) {
            descartarVencidos(fila, agora);
            for (Envio envio : fila) {
                if (envio.conteudo().equals(procurado)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void registrar(String contato, String conteudo) {
        String chave = chave(contato);
        if (chave.isEmpty() || conteudo.isEmpty()) {
            return;
        }
        Instant agora = clock.instant();
        if (envios.size() >= MAXIMO_CONTATOS) {
            limparVencidos(agora);
        }
        if (envios.size() >= MAXIMO_CONTATOS && !envios.containsKey(chave)) {
            return;
        }
        Deque<Envio> fila = envios.computeIfAbsent(chave, ignorado -> new ArrayDeque<>());
        synchronized (fila) {
            descartarVencidos(fila, agora);
            while (fila.size() >= MAXIMO_POR_CONTATO) {
                fila.pollFirst();
            }
            fila.addLast(new Envio(conteudo, agora));
        }
    }

    private void limparVencidos(Instant agora) {
        envios.forEach((chave, fila) -> {
            synchronized (fila) {
                descartarVencidos(fila, agora);
                if (fila.isEmpty()) {
                    envios.remove(chave, fila);
                }
            }
        });
    }

    private void descartarVencidos(Deque<Envio> fila, Instant agora) {
        while (!fila.isEmpty() && Duration.between(fila.peekFirst().momento(), agora).compareTo(VALIDADE) > 0) {
            fila.pollFirst();
        }
    }

    private String chave(String contato) {
        if (contato == null || contato.isBlank()) {
            return "";
        }
        return WhatsappUtil.normalizarParaChaveEstado(contato);
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }
}
