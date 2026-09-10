package com.example.demo.servico;

import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServicoWppConnectTest {

    private ServicoWppConnect servico;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        servico = new ServicoWppConnect(restTemplate);
        ReflectionTestUtils.setField(servico, "urlBase", "http://localhost:21465");
        ReflectionTestUtils.setField(servico, "secretKey", "secret");
        ReflectionTestUtils.setField(servico, "webhookUrl", "http://bot-java:8080/webhook/whatsapp");
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void mapeiaQrcodeDoWppConnect() {
        SessaoStatusResponseDTO dto = servico.paraDto(
                "sessao_recanto_01",
                Map.of("status", "QRCODE", "qrcode", "data:image/png;base64,abc")
        );
        assertEquals("QRCODE", dto.status());
        assertEquals("data:image/png;base64,abc", dto.qrcodeBase64());
    }

    @Test
    void mapeiaConnectedSemQr() {
        SessaoStatusResponseDTO dto = servico.paraDto("s1", Map.of("status", "CONNECTED"));
        assertEquals("CONNECTED", dto.status());
        assertNull(dto.qrcodeBase64());
    }

    @Test
    void mapeiaClosedParaDisconnected() {
        SessaoStatusResponseDTO dto = servico.paraDto("s1", Map.of("status", "CLOSED"));
        assertEquals("DISCONNECTED", dto.status());
    }

    @Test
    void mapeiaInitializingParaQrcodeEnquantoOWppGeraAImagem() {
        SessaoStatusResponseDTO dto = servico.paraDto("s1", Map.of("status", "INITIALIZING"));
        assertEquals("QRCODE", dto.status());
        assertNull(dto.qrcodeBase64());
    }

    @Test
    void consultaStatusSessionNoWppConnectEExtraiQr() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-wpp"))
                .andRespond(withSuccess(
                        "{\"status\":\"QRCODE\",\"qrcode\":\"data:image/png;base64,abc\"}",
                        MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.consultarStatus("sessao_recanto_01");

        assertEquals("QRCODE", dto.status());
        assertEquals("data:image/png;base64,abc", dto.qrcodeBase64());
        server.verify();
    }

    @Test
    void disparaStartSessionNoWppConnect() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"CLOSED\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/qrcode-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/start-session"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"webhook\":\"http://bot-java:8080/webhook/whatsapp\",\"waitQrCode\":false}"))
                .andRespond(withSuccess(
                        "{\"status\":\"qrcode\",\"qrcode\":\"data:image/png;base64,abc\"}",
                        MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.iniciarSessao("sessao_recanto_01");

        assertEquals("QRCODE", dto.status());
        assertEquals("data:image/png;base64,abc", dto.qrcodeBase64());
        server.verify();
    }

    @Test
    void iniciaSessaoSemQrImediatoMantemAguardandoLeitura() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"CLOSED\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/qrcode-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/start-session"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"status\":\"INITIALIZING\"}", MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.iniciarSessao("sessao_recanto_01");

        assertEquals("QRCODE", dto.status());
        assertNull(dto.qrcodeBase64());
        server.verify();
    }

    @Test
    void devolveConexaoExistenteSemReiniciarSessao() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"CONNECTED\"}", MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.iniciarSessao("sessao_recanto_01");

        assertEquals("CONNECTED", dto.status());
        assertNull(dto.qrcodeBase64());
        server.verify();
    }

    private void simularToken(String sessao) {
        server.expect(requestTo("http://localhost:21465/api/" + sessao + "/secret/generate-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"token-wpp\"}", MediaType.APPLICATION_JSON));
    }
}
