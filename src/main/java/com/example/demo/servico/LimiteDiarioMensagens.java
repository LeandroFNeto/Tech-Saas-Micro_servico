package com.example.demo.servico;

import com.example.demo.util.WhatsappUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteDiarioMensagens {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int TAMANHO_MAXIMO_MAPA = 5000;

    private record Contagem(LocalDate dia, int total) {
    }

    private final int limite;
    private final Clock clock;
    private final Map<String, Contagem> contagens = new ConcurrentHashMap<>();

    @Autowired
    public LimiteDiarioMensagens(@Value("${gamb.atendente.limite-diario-mensagens:30}") int limite) {
        this(limite, Clock.system(FUSO));
    }

    LimiteDiarioMensagens(int limite, Clock clock) {
        this.limite = limite;
        this.clock = clock;
    }

    public boolean registrarSePermitido(String numeroCliente) {
        String chave = WhatsappUtil.normalizarParaChaveEstado(numeroCliente);
        if (chave.isEmpty()) {
            return false;
        }
        LocalDate hoje = LocalDate.now(clock);
        if (contagens.size() > TAMANHO_MAXIMO_MAPA) {
            contagens.values().removeIf(contagem -> contagem.dia().isBefore(hoje));
        }
        boolean[] permitido = {false};
        contagens.compute(chave, (ignorado, atual) -> {
            Contagem base = atual == null || !atual.dia().equals(hoje) ? new Contagem(hoje, 0) : atual;
            if (base.total() >= limite) {
                return base;
            }
            permitido[0] = true;
            return new Contagem(hoje, base.total() + 1);
        });
        return permitido[0];
    }
}
