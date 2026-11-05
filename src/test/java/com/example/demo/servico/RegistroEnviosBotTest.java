package com.example.demo.servico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroEnviosBotTest {

    private static final String CONTATO = "5511988887777@c.us";

    private Instant agora;
    private RegistroEnviosBot registro;

    private Clock relogio() {
        return new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return agora;
            }
        };
    }

    @BeforeEach
    void preparar() {
        agora = Instant.parse("2026-11-04T12:00:00Z");
        registro = new RegistroEnviosBot(relogio());
    }

    @Test
    @DisplayName("Reconhece o texto que o bot enviou, ignorando espaços e quebras de linha")
    void reconheceTextoEnviado() {
        registro.registrarTexto(CONTATO, "Olá!\n\nComo posso   ajudar?");

        assertTrue(registro.foiEnviadoPeloBot(CONTATO, "Olá! Como posso ajudar?"));
    }

    @Test
    @DisplayName("Texto diferente ou de outro contato não é do bot")
    void naoReconheceTextoDiferente() {
        registro.registrarTexto(CONTATO, "Olá!");

        assertFalse(registro.foiEnviadoPeloBot(CONTATO, "Oi, sou eu digitando"));
        assertFalse(registro.foiEnviadoPeloBot("5511977776666@c.us", "Olá!"));
    }

    @Test
    @DisplayName("Mídia enviada pelo bot casa com evento sem texto")
    void reconheceMidia() {
        registro.registrarMidia(CONTATO);

        assertTrue(registro.foiEnviadoPeloBot(CONTATO, null));
        assertTrue(registro.foiEnviadoPeloBot(CONTATO, "  "));
    }

    @Test
    @DisplayName("Evento sem texto e sem mídia enviada não é do bot")
    void semMidiaRegistradaNaoCasa() {
        registro.registrarTexto(CONTATO, "Olá!");

        assertFalse(registro.foiEnviadoPeloBot(CONTATO, null));
    }

    @Test
    @DisplayName("O registro expira em 2 minutos")
    void expiraEmDoisMinutos() {
        registro.registrarTexto(CONTATO, "Olá!");

        agora = agora.plus(Duration.ofSeconds(119));
        assertTrue(registro.foiEnviadoPeloBot(CONTATO, "Olá!"));

        agora = agora.plus(Duration.ofSeconds(2));
        assertFalse(registro.foiEnviadoPeloBot(CONTATO, "Olá!"));
    }

    @Test
    @DisplayName("Respeita o limite de envios por contato descartando os mais antigos")
    void respeitaLimitePorContato() {
        for (int i = 0; i < RegistroEnviosBot.MAXIMO_POR_CONTATO + 1; i++) {
            registro.registrarTexto(CONTATO, "mensagem " + i);
        }

        assertFalse(registro.foiEnviadoPeloBot(CONTATO, "mensagem 0"));
        assertTrue(registro.foiEnviadoPeloBot(CONTATO, "mensagem " + RegistroEnviosBot.MAXIMO_POR_CONTATO));
    }

    @Test
    @DisplayName("Contato nulo ou em branco nunca casa")
    void contatoVazioNaoCasa() {
        registro.registrarTexto(null, "Olá!");

        assertFalse(registro.foiEnviadoPeloBot(null, "Olá!"));
        assertFalse(registro.foiEnviadoPeloBot(" ", "Olá!"));
    }

    @Test
    @DisplayName("Esquecer remove o registro de texto e mídia")
    void esquecerRemoveORegistro() {
        registro.registrarTexto(CONTATO, "Olá!");
        registro.registrarMidia(CONTATO);

        registro.esquecerTexto(CONTATO, "Olá!");
        registro.esquecerMidia(CONTATO);

        assertFalse(registro.foiEnviadoPeloBot(CONTATO, "Olá!"));
        assertFalse(registro.foiEnviadoPeloBot(CONTATO, ""));
    }
}
