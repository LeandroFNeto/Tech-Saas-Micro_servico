package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LiteralDeIpTest {

    @Test
    @DisplayName("IPv4 decimal com 4 octetos vira 4 bytes")
    void ipv4Valido() {
        assertArrayEquals(new byte[]{10, 0, 0, 1}, LiteralDeIp.bytes("10.0.0.1"));
        assertArrayEquals(new byte[]{0, 0, 0, 0}, LiteralDeIp.bytes("0.0.0.0"));
        assertArrayEquals(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255}, LiteralDeIp.bytes("255.255.255.255"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cafe.de", "dead.beef", "1.2.3", "1.2.3.4.5", "010.0.0.1", "01.1.1.1", "256.1.1.1",
            "1.1.1.-1", "1..1.1", "1.1.1.", ".1.1.1", "localhost", "", "0x7f.0.0.1", "1.1.1.1a", "1e2.1.1.1"})
    @DisplayName("Formas que nao sao IPv4 literal sao recusadas sem DNS")
    void ipv4Recusado(String texto) {
        assertNull(LiteralDeIp.bytes(texto));
    }

    @Test
    @DisplayName("IPv6 completo, comprimido e com IPv4 embutido")
    void ipv6Valido() {
        byte[] completo = LiteralDeIp.bytes("2001:db8:0:0:0:0:0:1");
        byte[] comprimido = LiteralDeIp.bytes("2001:db8::1");
        assertNotNull(completo);
        assertEquals(16, completo.length);
        assertArrayEquals(completo, comprimido);
        assertArrayEquals(new byte[16], LiteralDeIp.bytes("::"));
        byte[] laco = new byte[16];
        laco[15] = 1;
        assertArrayEquals(laco, LiteralDeIp.bytes("::1"));
        assertEquals(16, LiteralDeIp.bytes("64:ff9b::192.0.2.33").length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fe80::1%eth0", "fe80::1%1", "1::2::3", ":1:2:3:4:5:6:7", "1:2:3:4:5:6:7", "1:2:3:4:5:6:7:8:9",
            "12345::1", "g::1", "::1.2.3", "::010.0.0.1", "1:2:3:4:5:6:7::8", ":::", "1:::2"})
    @DisplayName("Formas invalidas de IPv6 sao recusadas, inclusive com zona")
    void ipv6Recusado(String texto) {
        assertNull(LiteralDeIp.bytes(texto));
    }

    @Test
    @DisplayName("IPv4 mapeado em IPv6 devolve os 4 bytes do IPv4")
    void ipv4MapeadoEmIpv6() {
        assertArrayEquals(new byte[]{10, 0, 0, 1}, LiteralDeIp.bytes("::ffff:10.0.0.1"));
        assertArrayEquals(new byte[]{10, 0, 0, 1}, LiteralDeIp.bytes("::FFFF:a00:1"));
        assertArrayEquals(new byte[]{10, 0, 0, 1}, LiteralDeIp.bytes("0:0:0:0:0:ffff:10.0.0.1"));
    }

    @Test
    @DisplayName("Entrada nula devolve nulo")
    void entradaNula() {
        assertNull(LiteralDeIp.bytes(null));
    }
}
