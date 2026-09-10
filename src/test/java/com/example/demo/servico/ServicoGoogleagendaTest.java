package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServicoGoogleagendaTest {

    @Test
    @DisplayName("Com credencial.json a agenda inicializa e fica disponível em memória")
    void inicializarComCredencialConectaAgenda() {
        ServicoGoogleagenda servico = new ServicoGoogleagenda();
        assertDoesNotThrow(servico::inicializarAgenda);
        assertNotNull(servico.conectarAgenda());
    }
}
