package com.example.demo.servico;

public class AgendaNaoConfiguradaException extends RuntimeException {

    public AgendaNaoConfiguradaException(Long empresaId) {
        super("Empresa " + empresaId + " sem agenda cadastrada");
    }
}
