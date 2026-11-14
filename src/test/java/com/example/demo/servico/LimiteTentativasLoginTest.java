package com.example.demo.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteTentativasLoginTest {

    private static final class RelogioMutavel extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

        void avancarMinutos(long minutos) {
            instante.updateAndGet(i -> i.plusSeconds(minutos * 60));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    @Test
    @DisplayName("Bloqueia a sexta tentativa com tempo de espera e libera depois da janela")
    void bloqueiaAposCincoFalhas() {
        RelogioMutavel relogio = new RelogioMutavel();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);
        for (int i = 0; i < 5; i++) {
            limite.exigirLivre("a@b.com", "1.1.1.1");
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }

        LoginBloqueadoException bloqueio = assertThrows(LoginBloqueadoException.class,
                () -> limite.exigirLivre("A@b.com", "1.1.1.1"));
        assertTrue(bloqueio.getSegundosParaTentarDeNovo() > 0);

        relogio.avancarMinutos(16);
        assertDoesNotThrow(() -> limite.exigirLivre("a@b.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("A contagem é por e-mail e IP")
    void contagemPorEmailEIp() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel());
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }
        assertDoesNotThrow(() -> limite.exigirLivre("a@b.com", "2.2.2.2"));
        assertDoesNotThrow(() -> limite.exigirLivre("c@d.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("Login certo zera a contagem")
    void zeraContagem() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel());
        for (int i = 0; i < 4; i++) {
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }
        limite.zerar("a@b.com", "1.1.1.1");
        limite.registrarFalha("a@b.com", "1.1.1.1");
        assertDoesNotThrow(() -> limite.exigirLivre("a@b.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("Teto de entradas: a falha mais antiga sai e o total continua em 10.000")
    void tetoDeEntradasDescartaAMaisAntiga() {
        RelogioMutavel relogio = new RelogioMutavel();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);
        for (int i = 0; i < LimiteTentativasLogin.MAXIMO_ENTRADAS; i++) {
            limite.registrarFalha("u" + i + "@b.com", "1.1.1.1");
        }
        assertEquals(LimiteTentativasLogin.MAXIMO_ENTRADAS, limite.totalDeEntradas());

        limite.registrarFalha("novo@b.com", "1.1.1.1");
        assertEquals(LimiteTentativasLogin.MAXIMO_ENTRADAS, limite.totalDeEntradas());

        for (int i = 0; i < LimiteTentativasLogin.MAXIMO_FALHAS - 1; i++) {
            limite.registrarFalha("u0@b.com", "1.1.1.1");
        }
        assertDoesNotThrow(() -> limite.exigirLivre("u0@b.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("Entradas vencidas saem do inicio no mesmo passo da nova falha")
    void entradasVencidasSaem() {
        RelogioMutavel relogio = new RelogioMutavel();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);
        for (int i = 0; i < 100; i++) {
            limite.registrarFalha("v" + i + "@b.com", "1.1.1.1");
        }
        relogio.avancarMinutos(16);
        limite.registrarFalha("atual@b.com", "1.1.1.1");
        assertEquals(1, limite.totalDeEntradas());
    }

    @Test
    @DisplayName("A fila de cada chave guarda no maximo MAXIMO_FALHAS registros")
    void filaLimitadaAoMaximoDeFalhas() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel());
        for (int i = 0; i < 50; i++) {
            limite.registrarFalha("a@b.com", "1.1.1.1");
        }
        assertEquals(1, limite.totalDeEntradas());
        assertThrows(LoginBloqueadoException.class, () -> limite.exigirLivre("a@b.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("Com limite por IP ligado, 30 falhas do mesmo IP com e-mails diferentes bloqueiam o IP")
    void limitePorIpBloqueiaEmailsDiferentes() {
        RelogioMutavel relogio = new RelogioMutavel();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio, true);
        for (int i = 0; i < LimiteTentativasLogin.MAXIMO_FALHAS_POR_IP; i++) {
            limite.exigirLivre("m" + i + "@b.com", "9.9.9.9");
            limite.registrarFalha("m" + i + "@b.com", "9.9.9.9");
        }
        LoginBloqueadoException bloqueio = assertThrows(LoginBloqueadoException.class,
                () -> limite.exigirLivre("outro@b.com", "9.9.9.9"));
        assertTrue(bloqueio.getSegundosParaTentarDeNovo() > 0);
        assertDoesNotThrow(() -> limite.exigirLivre("outro@b.com", "8.8.8.8"));

        relogio.avancarMinutos(16);
        assertDoesNotThrow(() -> limite.exigirLivre("outro@b.com", "9.9.9.9"));
    }

    @Test
    @DisplayName("Sem limite por IP (lista de proxies vazia), muitos e-mails do mesmo IP nao bloqueiam")
    void semLimitePorIpNaoBloqueia() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel(), false);
        for (int i = 0; i < 40; i++) {
            limite.registrarFalha("m" + i + "@b.com", "9.9.9.9");
        }
        assertDoesNotThrow(() -> limite.exigirLivre("outro@b.com", "9.9.9.9"));
    }

    @Test
    @DisplayName("Construtor Spring liga o limite por IP so com PROXIES_CONFIAVEIS preenchida")
    void construtorSpringLigaLimitePorIp() {
        LimiteTentativasLogin vazio = new LimiteTentativasLogin("");
        LimiteTentativasLogin preenchido = new LimiteTentativasLogin("10.0.0.5");
        for (int i = 0; i < 40; i++) {
            vazio.registrarFalha("m" + i + "@b.com", "9.9.9.9");
            preenchido.registrarFalha("m" + i + "@b.com", "9.9.9.9");
        }
        assertDoesNotThrow(() -> vazio.exigirLivre("outro@b.com", "9.9.9.9"));
        assertThrows(LoginBloqueadoException.class, () -> preenchido.exigirLivre("outro@b.com", "9.9.9.9"));
    }

    @Test
    @DisplayName("Login certo zera a contagem por e-mail e IP mas nao a contagem por IP")
    void loginCertoNaoZeraContagemPorIp() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(new RelogioMutavel(), true);
        for (int i = 0; i < LimiteTentativasLogin.MAXIMO_FALHAS_POR_IP; i++) {
            limite.registrarFalha("m" + i + "@b.com", "9.9.9.9");
        }
        limite.zerar("m0@b.com", "9.9.9.9");
        assertThrows(LoginBloqueadoException.class, () -> limite.exigirLivre("m0@b.com", "9.9.9.9"));
    }
}
