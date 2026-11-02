package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicoGoogleagendaTest {

    @Test
    @DisplayName("Inicializacao da agenda lida graciosamente com presenca ou ausencia de credencial.json")
    void inicializarComCredencialConectaAgenda() {
        ServicoGoogleagenda servico = new ServicoGoogleagenda();
        assertDoesNotThrow(servico::inicializarAgenda);

        boolean temCredencial = ServicoGoogleagenda.class.getResourceAsStream("/credencial.json") != null;
        if (temCredencial) {
            assertNotNull(servico.conectarAgenda());
        } else {
            assertThrows(IllegalStateException.class, servico::conectarAgenda);
        }
    }
}
