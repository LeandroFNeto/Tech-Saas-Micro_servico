package com.example.demo.servico;

import com.example.demo.dto.whatsapp.CodigoPareamentoResponseDTO;
import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
    void repassaInitializingSemConverterEmQrcode() {
        SessaoStatusResponseDTO dto = servico.paraDto("s1", Map.of("status", "INITIALIZING"));
        assertEquals("INITIALIZING", dto.status());
        assertNull(dto.qrcodeBase64());
    }

    @Test
    void consultaDuranteReconexaoDevolveInitializingSemBuscarQr() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"INITIALIZING\",\"qrcode\":null}", MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.consultarStatus("sessao_recanto_01");

        assertEquals("INITIALIZING", dto.status());
        assertNull(dto.qrcodeBase64());
        server.verify();
    }

    @Test
    void startSessionDuranteReconexaoRepassaInitializingSemQr() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"DISCONNECTED\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/qrcode-session"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/start-session"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"status\":\"INITIALIZING\"}", MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.iniciarSessao("sessao_recanto_01");

        assertEquals("INITIALIZING", dto.status());
        assertNull(dto.qrcodeBase64());
        server.verify();
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
                .andRespond(withSuccess("{\"status\":\"qrcode\"}", MediaType.APPLICATION_JSON));

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

    @Test
    void lerSaudeDoGatewayComContagensENomes() {
        server.expect(requestTo("http://localhost:21465/api/health"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer secret"))
                .andRespond(withSuccess(
                        "{\"status\":\"ok\",\"sessoes\":{\"conectadas\":2,\"aguardandoQr\":1,\"desconectadas\":[\"sessao_x\"],\"nomesConectadas\":[\"sessao_y\",\"sessao_z\"]}}",
                        MediaType.APPLICATION_JSON));

        ServicoWppConnect.SaudeGateway saude = servico.consultarSaude();

        assertEquals(ServicoWppConnect.SaudeGateway.Situacao.OK, saude.situacao());
        assertEquals(2, saude.conectadas());
        assertEquals(1, saude.aguardandoQr());
        assertEquals(java.util.List.of("sessao_x"), saude.desconectadas());
        assertEquals(java.util.List.of("sessao_y", "sessao_z"), saude.nomesConectadas());
        server.verify();
    }

    @Test
    void gatewayAntigoSemNomesConectadasEhTolerado() {
        server.expect(requestTo("http://localhost:21465/api/health"))
                .andRespond(withSuccess(
                        "{\"status\":\"ok\",\"sessoes\":{\"conectadas\":1,\"aguardandoQr\":0,\"desconectadas\":[]}}",
                        MediaType.APPLICATION_JSON));

        ServicoWppConnect.SaudeGateway saude = servico.consultarSaude();

        assertEquals(ServicoWppConnect.SaudeGateway.Situacao.OK, saude.situacao());
        assertNull(saude.nomesConectadas());
    }

    @Test
    void saudeComRota404EhGatewaySemRota() {
        server.expect(requestTo("http://localhost:21465/api/health"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertEquals(ServicoWppConnect.SaudeGateway.Situacao.SEM_ROTA, servico.consultarSaude().situacao());
    }

    @Test
    void saudeSemRespostaEhIndisponivel() {
        server.expect(requestTo("http://localhost:21465/api/health"))
                .andRespond(withServerError());

        assertEquals(ServicoWppConnect.SaudeGateway.Situacao.INDISPONIVEL, servico.consultarSaude().situacao());
    }

    @Test
    void geraCodigoDePareamentoEnviandoSoOsDigitos() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-wpp"))
                .andExpect(content().json("{\"phone\":\"5511999990000\"}"))
                .andRespond(withSuccess("{\"code\":\"ABCD-EFGH\"}", MediaType.APPLICATION_JSON));

        CodigoPareamentoResponseDTO dto = servico.solicitarCodigoPareamento("sessao_recanto_01", "5511999990000");

        assertEquals("sessao_recanto_01", dto.sessao());
        assertEquals("ABCD-EFGH", dto.codigo());
        server.verify();
    }

    @Test
    void repassaConflitoELimiteDoGatewayNoCodigoDePareamento() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertEquals(HttpStatus.CONFLICT,
                statusDe(() -> servico.solicitarCodigoPareamento("sessao_recanto_01", "5511999990000")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                statusDe(() -> servico.solicitarCodigoPareamento("sessao_recanto_01", "5511999990000")));
    }

    @Test
    void gatewaySemRotaDeCodigoResponde503() {
        simularToken("sessao_recanto_01");
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.solicitarCodigoPareamento("sessao_recanto_01", "5511999990000"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        assertEquals("pareamento por código indisponível", erro.getReason());
    }

    @Test
    void renovaOTokenEmCasoDe401AoPedirCodigo() {
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/secret/generate-token"))
                .andRespond(withSuccess("{\"token\":\"velho\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andExpect(header("Authorization", "Bearer velho"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/secret/generate-token"))
                .andRespond(withSuccess("{\"token\":\"novo\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andExpect(header("Authorization", "Bearer novo"))
                .andRespond(withSuccess("{\"code\":\"ZZZZ-9999\"}", MediaType.APPLICATION_JSON));

        assertEquals("ZZZZ-9999", servico.solicitarCodigoPareamento("sessao_recanto_01", "5511999990000").codigo());
        server.verify();
    }

    private HttpStatus statusDe(Runnable acao) {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, acao::run);
        return HttpStatus.valueOf(erro.getStatusCode().value());
    }

    private void simularToken(String sessao) {
        server.expect(requestTo("http://localhost:21465/api/" + sessao + "/secret/generate-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"token-wpp\"}", MediaType.APPLICATION_JSON));
    }
}
