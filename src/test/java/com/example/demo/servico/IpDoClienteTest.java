package com.example.demo.servico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpDoClienteTest {

    private MockHttpServletRequest requisicao(String remoto, String encaminhado) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setRemoteAddr(remoto);
        if (encaminhado != null) {
            requisicao.addHeader("X-Forwarded-For", encaminhado);
        }
        return requisicao;
    }

    @Test
    @DisplayName("Proxy confiável: usa o IP do X-Forwarded-For")
    void proxyConfiavelUsaCabecalho() {
        IpDoCliente ip = new IpDoCliente("10.0.0.5");
        assertEquals("203.0.113.9", ip.resolver(requisicao("10.0.0.5", "203.0.113.9")));
    }

    @Test
    @DisplayName("Proxy confiável por CIDR: usa o IP do X-Forwarded-For")
    void proxyConfiavelPorCidr() {
        IpDoCliente ip = new IpDoCliente("172.16.0.0/12");
        assertEquals("203.0.113.9", ip.resolver(requisicao("172.18.0.3", "203.0.113.9")));
    }

    @Test
    @DisplayName("Proxy não confiável: ignora o cabeçalho")
    void proxyNaoConfiavelIgnoraCabecalho() {
        IpDoCliente ip = new IpDoCliente("10.0.0.5");
        assertEquals("198.51.100.7", ip.resolver(requisicao("198.51.100.7", "203.0.113.9")));
    }

    @Test
    @DisplayName("Sem lista de proxies: usa o endereço remoto")
    void semListaUsaEnderecoRemoto() {
        IpDoCliente ip = new IpDoCliente("");
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "203.0.113.9")));
    }

    @Test
    @DisplayName("Cadeia com IP forjado à esquerda: usa o primeiro não confiável da direita")
    void cadeiaIgnoraIpForjado() {
        IpDoCliente ip = new IpDoCliente("10.0.0.5");
        assertEquals("203.0.113.9", ip.resolver(requisicao("10.0.0.5", "1.1.1.1, 203.0.113.9")));
    }

    @Test
    @DisplayName("Cabeçalho inválido volta para o endereço remoto")
    void cabecalhoInvalidoUsaEnderecoRemoto() {
        IpDoCliente ip = new IpDoCliente("10.0.0.5");
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "nao-e-ip")));
    }

    @Test
    @DisplayName("Nome de host no X-Forwarded-For e recusado e usa o IP da conexao")
    void nomeDeHostNoCabecalhoUsaEnderecoRemoto() {
        IpDoCliente ip = new IpDoCliente("10.0.0.5");
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "cafe.de")));
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "dead.beef")));
    }

    @Test
    @DisplayName("Formas abreviadas ou com zero a esquerda no cabecalho voltam para o IP da conexao")
    void formasAbreviadasNoCabecalho() {
        IpDoCliente ip = new IpDoCliente("10.0.0.5");
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "1.2.3")));
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "010.0.0.1")));
        assertEquals("10.0.0.5", ip.resolver(requisicao("10.0.0.5", "fe80::1%eth0")));
    }

    @Test
    @DisplayName("Conexao em IPv6 mapeado de IPv4 e reconhecida como o proxy IPv4")
    void ipv4DentroDeIpv6EProxyConfiavel() {
        IpDoCliente ip = new IpDoCliente("10.0.0.1");
        assertEquals("203.0.113.9", ip.resolver(requisicao("::ffff:10.0.0.1", "203.0.113.9")));
    }

    @Test
    @DisplayName("Entrada de proxy que e nome de host ou abreviada e ignorada")
    void entradaDeProxyInvalidaEIgnorada() {
        IpDoCliente ip = new IpDoCliente("cafe.de, 1.2.3, 10.0.0.5");
        assertEquals("203.0.113.9", ip.resolver(requisicao("10.0.0.5", "203.0.113.9")));
        IpDoCliente soInvalidas = new IpDoCliente("cafe.de");
        assertEquals("10.0.0.5", soInvalidas.resolver(requisicao("10.0.0.5", "203.0.113.9")));
    }

    @Test
    @DisplayName("Lista vazia de proxies registra aviso no boot sem expor valor")
    void listaVaziaRegistraAviso() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(IpDoCliente.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> captura = new ch.qos.logback.core.read.ListAppender<>();
        captura.start();
        logger.addAppender(captura);
        try {
            new IpDoCliente("");
            assertEquals(1, captura.list.stream().filter(e -> e.getLevel() == ch.qos.logback.classic.Level.WARN).count());
            captura.list.clear();
            new IpDoCliente("10.0.0.5");
            assertEquals(0, captura.list.size());
        } finally {
            logger.detachAppender(captura);
        }
    }
}
