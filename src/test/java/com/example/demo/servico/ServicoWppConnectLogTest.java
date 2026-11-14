package com.example.demo.servico;

import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(OutputCaptureExtension.class)
class ServicoWppConnectLogTest {

    private static final String CORPO_SIGILOSO = "{\"erro\":\"cliente Joana Prado numero 5511999997777\"}";

    private ServicoWppConnect servico;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        servico = new ServicoWppConnect(restTemplate);
        ReflectionTestUtils.setField(servico, "urlBase", "http://localhost:21465");
        ReflectionTestUtils.setField(servico, "secretKey", "secret-key-de-teste");
        ReflectionTestUtils.setField(servico, "webhookUrl", "http://bot-java:8080/webhook/whatsapp");
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("Falha ao obter token registra sessão e classe, sem corpo da resposta nem a chave secreta")
    void falhaNoTokenNaoVazaCorpoNemSegredo(CapturedOutput saida) {
        server.expect(manyTimes(), requestTo("http://localhost:21465/api/sessao_recanto_01/secret-key-de-teste/generate-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR).body(CORPO_SIGILOSO).contentType(MediaType.APPLICATION_JSON));

        String token = servico.obterToken("sessao_recanto_01");

        assertThat(token).isEmpty();
        assertThat(saida.getAll())
                .contains("sessao_recanto_01")
                .contains("InternalServerError")
                .doesNotContain("Joana Prado")
                .doesNotContain("5511999997777")
                .doesNotContain("secret-key-de-teste");
    }

    @Test
    @DisplayName("Falha ao consultar status registra sessão e classe, sem corpo da resposta")
    void falhaNoStatusNaoVazaCorpo(CapturedOutput saida) {
        server.expect(manyTimes(), requestTo("http://localhost:21465/api/sessao_recanto_01/secret-key-de-teste/generate-token"))
                .andRespond(withServerError().body(CORPO_SIGILOSO).contentType(MediaType.APPLICATION_JSON));
        server.expect(manyTimes(), requestTo("http://localhost:21465/api/sessao_recanto_01/status-session"))
                .andRespond(withServerError().body(CORPO_SIGILOSO).contentType(MediaType.APPLICATION_JSON));

        SessaoStatusResponseDTO dto = servico.consultarStatus("sessao_recanto_01");

        assertThat(dto.status()).isEqualTo("DISCONNECTED");
        assertThat(saida.getAll())
                .contains("sessao_recanto_01")
                .contains("InternalServerError")
                .doesNotContain("Joana Prado")
                .doesNotContain("5511999997777");
    }

    @Test
    @DisplayName("Pedido de código de pareamento loga o telefone mascarado e nunca o código nem a chave")
    void codigoDePareamentoNaoVazaTelefoneNemCodigo(CapturedOutput saida) {
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/secret-key-de-teste/generate-token"))
                .andRespond(withSuccess("{\"token\":\"token-wpp\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:21465/api/sessao_recanto_01/request-pairing-code"))
                .andRespond(withSuccess("{\"code\":\"ABCD-EFGH\"}", MediaType.APPLICATION_JSON));

        servico.solicitarCodigoPareamento("sessao_recanto_01", "5511999997777");

        assertThat(saida.getAll())
                .contains("sessao_recanto_01")
                .contains("***7777")
                .doesNotContain("5511999997777")
                .doesNotContain("ABCD-EFGH")
                .doesNotContain("token-wpp")
                .doesNotContain("secret-key-de-teste");
    }

    @Test
    @DisplayName("Falha na rota de saúde não loga a chave secreta enviada no cabeçalho")
    void saudeNaoVazaChaveSecreta(CapturedOutput saida) {
        server.expect(requestTo("http://localhost:21465/api/health"))
                .andRespond(withServerError().body(CORPO_SIGILOSO).contentType(MediaType.APPLICATION_JSON));

        servico.consultarSaude();

        assertThat(saida.getAll())
                .doesNotContain("secret-key-de-teste")
                .doesNotContain("Joana Prado")
                .doesNotContain("5511999997777");
    }
}
