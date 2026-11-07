package com.example.demo.strategy.experimental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class RegistroModulosExperimentais {

    private final Map<String, ModuloExperimental> modulos = new LinkedHashMap<>();

    @Autowired
    public RegistroModulosExperimentais(List<ModuloExperimental> beans) {
        for (ModuloExperimental modulo : beans) {
            modulos.put(chave(modulo.nome()), modulo);
        }
    }

    public List<ModuloExperimental> listar() {
        return List.copyOf(modulos.values());
    }

    public Optional<ModuloExperimental> buscar(String nome) {
        return Optional.ofNullable(modulos.get(chave(nome)));
    }

    private static String chave(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
    }
}
