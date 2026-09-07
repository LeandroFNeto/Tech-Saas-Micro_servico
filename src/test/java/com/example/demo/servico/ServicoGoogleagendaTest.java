package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicoGoogleagendaTest {

    @Test
    @DisplayName("Sem crendecial.json a inicialização só avisa e não derruba o contexto")
    void inicializarSemCredencialNaoLanca() {
        ServicoGoogleagenda servico = new ServicoGoogleagenda();
        assertDoesNotThrow(servico::inicializarAgenda);
        assertThrows(IllegalStateException.class, servico::conectarAgenda);
    }
}
