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
}
