package com.example.demo.servico;

import com.example.demo.util.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LimiteTentativasLogin {

    static final int MAXIMO_FALHAS = 5;
    static final int MAXIMO_FALHAS_POR_IP = 30;
    static final int MAXIMO_ENTRADAS = 10_000;
    static final Duration JANELA = Duration.ofMinutes(15);

    private static final String PREFIXO_IP = "ip:";
    private static final Logger log = LoggerFactory.getLogger(LimiteTentativasLogin.class);

    private final Map<String, Deque<Long>> falhas = new LinkedHashMap<>();
    private final Clock clock;
    private final boolean limitePorIp;

    @Autowired
    public LimiteTentativasLogin(@Value("${seguranca.proxies-confiaveis:}") String proxiesConfiaveis) {
        this(Clock.systemUTC(), proxiesConfiaveis != null && !proxiesConfiaveis.isBlank());
    }

    LimiteTentativasLogin(Clock clock) {
        this(clock, false);
    }

    LimiteTentativasLogin(Clock clock, boolean limitePorIp) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.limitePorIp = limitePorIp;
    }

    public synchronized void exigirLivre(String email, String ip) {
        long agora = clock.millis();
        long espera = 0;
        if (limitePorIp) {
            espera = esperaDe(chaveIp(ip), MAXIMO_FALHAS_POR_IP, agora);
        }
        if (espera == 0) {
            espera = esperaDe(chave(email, ip), MAXIMO_FALHAS, agora);
        }
        if (espera > 0) {
            log.warn("Login bloqueado por excesso de falhas para {}.", MascaraLog.email(email));
            throw new LoginBloqueadoException(espera);
        }
    }

    public synchronized void registrarFalha(String email, String ip) {
        long agora = clock.millis();
        registrar(chave(email, ip), MAXIMO_FALHAS, agora);
        if (limitePorIp) {
            registrar(chaveIp(ip), MAXIMO_FALHAS_POR_IP, agora);
        }
        descartarVencidasDoInicio(agora);
        Iterator<String> maisAntigas = falhas.keySet().iterator();
        while (falhas.size() > MAXIMO_ENTRADAS && maisAntigas.hasNext()) {
            maisAntigas.next();
            maisAntigas.remove();
        }
    }

    public synchronized void zerar(String email, String ip) {
        falhas.remove(chave(email, ip));
    }

    synchronized int totalDeEntradas() {
        return falhas.size();
    }

    private long esperaDe(String chave, int maximo, long agora) {
        Deque<Long> fila = falhas.get(chave);
        if (fila == null) {
            return 0;
        }
        descartarVencidas(fila, agora);
        if (fila.isEmpty()) {
            falhas.remove(chave);
            return 0;
        }
        if (fila.size() < maximo) {
            return 0;
        }
        long libera = fila.peekFirst() + JANELA.toMillis();
        return Math.max(1, (libera - agora + 999) / 1000);
    }

    private void registrar(String chave, int maximo, long agora) {
        Deque<Long> fila = falhas.remove(chave);
        if (fila == null) {
            fila = new ArrayDeque<>();
        }
        descartarVencidas(fila, agora);
        fila.addLast(agora);
        while (fila.size() > maximo) {
            fila.pollFirst();
        }
        falhas.put(chave, fila);
    }

    private void descartarVencidasDoInicio(long agora) {
        long inicio = agora - JANELA.toMillis();
        Iterator<Deque<Long>> iterador = falhas.values().iterator();
        while (iterador.hasNext()) {
            Deque<Long> fila = iterador.next();
            if (fila.isEmpty() || fila.peekLast() <= inicio) {
                iterador.remove();
            } else {
                break;
            }
        }
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

    private static String chaveIp(String ip) {
        return PREFIXO_IP + (ip == null ? "desconhecido" : ip);
    }
}
