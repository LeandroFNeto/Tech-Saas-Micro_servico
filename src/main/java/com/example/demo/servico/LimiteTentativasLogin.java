package com.example.demo.servico;

import com.example.demo.util.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteTentativasLogin {

    static final int MAXIMO_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(LimiteTentativasLogin.class);

    private final Map<String, Deque<Long>> falhas = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimiteTentativasLogin() {
        this(Clock.systemUTC());
    }

    LimiteTentativasLogin(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public void exigirLivre(String email, String ip) {
        long agora = clock.millis();
        long[] espera = {0};

        falhas.computeIfPresent(chave(email, ip), (k, fila) -> {
            descartarVencidas(fila, agora);
            if (fila.isEmpty()) {
                return null;
            }
            if (fila.size() >= MAXIMO_FALHAS) {
                long libera = fila.peekFirst() + JANELA.toMillis();
                espera[0] = Math.max(1, (libera - agora + 999) / 1000);
            }
            return fila;
        });

        if (espera[0] > 0) {
            log.warn("Login bloqueado por excesso de falhas para {}.", MascaraLog.email(email));
            throw new LoginBloqueadoException(espera[0]);
        }
    }

    public void registrarFalha(String email, String ip) {
        long agora = clock.millis();
        falhas.compute(chave(email, ip), (k, fila) -> {
            Deque<Long> atual = fila == null ? new ArrayDeque<>() : fila;
            descartarVencidas(atual, agora);
            atual.addLast(agora);
            return atual;
        });
        if (falhas.size() > 10_000) {
            falhas.forEach((k, ignorado) -> falhas.computeIfPresent(k, (ch, fila) -> {
                descartarVencidas(fila, agora);
                return fila.isEmpty() ? null : fila;
            }));
        }
    }

    public void zerar(String email, String ip) {
        falhas.remove(chave(email, ip));
    }

    private void descartarVencidas(Deque<Long> fila, long agora) {
        long inicio = agora - JANELA.toMillis();
        while (!fila.isEmpty() && fila.peekFirst() <= inicio) {
            fila.pollFirst();
        }
    }

    private static String chave(String email, String ip) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalizado + "|" + (ip == null ? "desconhecido" : ip);
    }
}
