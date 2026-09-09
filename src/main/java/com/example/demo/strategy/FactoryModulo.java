package com.example.demo.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FactoryModulo {

    private final Map<String, ModuloAtendimentoStrategy> estrategias;

    @Autowired
    public FactoryModulo(List<ModuloAtendimentoStrategy> listaDeEstrategias) {
        this.estrategias = listaDeEstrategias.stream()
                .collect(Collectors.toMap(
                        estrategia -> normalizarRamo(estrategia.getRamoDeAtuacao()),
                        estrategia -> estrategia));
    }

    public ModuloAtendimentoStrategy obterEstrategia(String ramo) {
        String chave = normalizarRamo(ramo);
        ModuloAtendimentoStrategy estrategia = chave.isEmpty() ? null : estrategias.get(chave);

        if (estrategia == null) {
            throw new IllegalArgumentException("Ramo de atuação não suportado ou em construção: " + ramo);
        }

        return estrategia;
    }

    static String normalizarRamo(String ramo) {
        if (ramo == null || ramo.isBlank()) {
            return "";
        }
        String semAcento = Normalizer.normalize(ramo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
