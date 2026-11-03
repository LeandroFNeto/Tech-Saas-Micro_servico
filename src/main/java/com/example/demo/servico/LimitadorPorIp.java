package com.example.demo.servico;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorPorIp {

    static final int LIMITE_POR_JANELA = 10;
    static final Duration JANELA = Duration.ofMinutes(1);

    private final Map<String, Deque<Long>> acessos = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimitadorPorIp() {
        this(Clock.systemUTC());
    }

    LimitadorPorIp(Clock clock) {
        this.clock = clock;
    }

    public boolean permitir(String ip) {
        long agora = clock.millis();
        long inicioJanela = agora - JANELA.toMillis();
        boolean[] permitido = {false};

        acessos.compute(ip == null ? "desconhecido" : ip, (chave, fila) -> {
            Deque<Long> recentes = fila == null ? new ArrayDeque<>() : fila;
            while (!recentes.isEmpty() && recentes.peekFirst() <= inicioJanela) {
                recentes.pollFirst();
            }
            if (recentes.size() < LIMITE_POR_JANELA) {
                recentes.addLast(agora);
                permitido[0] = true;
            }
            return recentes;
        });

        if (acessos.size() > 10_000) {
            acessos.values().removeIf(fila -> fila.isEmpty() || fila.peekLast() <= inicioJanela);
        }
        return permitido[0];
    }
}
