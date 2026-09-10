package com.example.demo.servico;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicoAuthEmailTest {

    @Test
    void deveGerarEmailPelaSessaoEmMinusculasSemEspacos() {
        assertEquals(
                "sessaorecanto01@gamb.com.br",
                ServicoAuth.emailAcessoDaSessao("  Sessao Recanto 01 "));
    }

    @Test
    void deveRejeitarSessaoVazia() {
        assertThrows(ResponseStatusException.class, () -> ServicoAuth.emailAcessoDaSessao("   "));
    }
}
