package com.example.demo.servico;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheRespostaIA {

    private static final int TAMANHO_MAXIMO = 300;

    private record Entrada(String resposta, Instant expiraEm) {
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final long validadeMinutos;
    private final Clock relogio;

    @Autowired
    public CacheRespostaIA(@Value("${gemini.cache-minutos:360}") long validadeMinutos) {
        this(validadeMinutos, Clock.systemUTC());
    }

    CacheRespostaIA(long validadeMinutos, Clock relogio) {
        this.validadeMinutos = validadeMinutos;
        this.relogio = relogio;
    }

    public Optional<String> obter(String prompt) {
        if (validadeMinutos <= 0 || prompt == null) {
            return Optional.empty();
        }
        String chave = chave(prompt);
        Entrada entrada = entradas.get(chave);
        if (entrada == null) {
            return Optional.empty();
        }
        if (!relogio.instant().isBefore(entrada.expiraEm())) {
            entradas.remove(chave);
            return Optional.empty();
        }
        return Optional.of(entrada.resposta());
    }

    public void guardar(String prompt, String resposta) {
        if (validadeMinutos <= 0 || prompt == null || resposta == null || resposta.isBlank()) {
            return;
        }
        if (entradas.size() >= TAMANHO_MAXIMO) {
            Instant agora = relogio.instant();
            entradas.values().removeIf(e -> !agora.isBefore(e.expiraEm()));
            if (entradas.size() >= TAMANHO_MAXIMO) {
                entradas.clear();
            }
        }
        entradas.put(chave(prompt), new Entrada(resposta, relogio.instant().plusSeconds(validadeMinutos * 60)));
    }

    private String chave(String prompt) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(prompt.hashCode());
        }
    }
}
