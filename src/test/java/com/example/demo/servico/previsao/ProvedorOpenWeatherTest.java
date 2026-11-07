package com.example.demo.servico.previsao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProvedorOpenWeatherTest {

    private MockRestServiceServer servidorDe(ProvedorOpenWeather provedor) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(provedor, "restTemplate");
        return MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("geocodificar filtra país BR, monta nome resolvido com estado e ignora itens fora do Brasil")
    void deveGeocodificarCidadeBrasileira() {
        ProvedorOpenWeather provedor = new ProvedorOpenWeather("chave-teste", "https://owm.local", 2000);
        MockRestServiceServer server = servidorDe(provedor);

        server.expect(requestTo("https://owm.local/geo/1.0/direct?q=Atibaia,BR&limit=5&appid=chave-teste"))
                .andRespond(withSuccess("""
                        [
                          {"name":"Atibaia","country":"PT","lat":39.0,"lon":-9.0},
                          {"name":"Atibaia","state":"São Paulo","country":"BR","lat":-23.117,"lon":-46.55}
                        ]
                        """, MediaType.APPLICATION_JSON));

        Optional<ProvedorPrevisao.Localizacao> loc = provedor.geocodificar("Atibaia");

        assertTrue(loc.isPresent());
        assertEquals(-23.117, loc.get().latitude(), 0.0001);
        assertEquals(-46.55, loc.get().longitude(), 0.0001);
        assertEquals("Atibaia, São Paulo, BR", loc.get().nomeResolvido());
        server.verify();
    }

    @Test
    @DisplayName("geocodificar retorna Optional.empty quando nenhum item do Brasil válido é retornado")
    void deveRetornarVazioQuandoSemCorrespondenciaNoBrasil() {
        ProvedorOpenWeather provedor = new ProvedorOpenWeather("chave-teste", "https://owm.local", 2000);
        MockRestServiceServer server = servidorDe(provedor);

        server.expect(requestTo("https://owm.local/geo/1.0/direct?q=CidadeX,BR&limit=5&appid=chave-teste"))
                .andRespond(withSuccess("[{\"name\":\"CidadeX\",\"country\":\"US\",\"lat\":10.0,\"lon\":20.0}]",
                        MediaType.APPLICATION_JSON));

        assertTrue(provedor.geocodificar("CidadeX").isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Chave ausente lança ProvedorIndisponivelException sem fazer chamada HTTP")
    void deveFalharQuandoChaveAusente() {
        ProvedorOpenWeather provedor = new ProvedorOpenWeather("   ", "https://owm.local", 2000);

        assertThrows(ProvedorIndisponivelException.class, () -> provedor.geocodificar("Atibaia"));
        assertThrows(ProvedorIndisponivelException.class, () -> provedor.previsaoDiaria(-23.0, -46.0));
    }

    @Test
    @DisplayName("Falha HTTP ou JSON fora do formato esperado lança ProvedorIndisponivelException")
    void deveLancarExcecaoEmErroHttpOuJsonInvalido() {
        ProvedorOpenWeather provedor = new ProvedorOpenWeather("chave-teste", "https://owm.local", 2000);
        MockRestServiceServer server = servidorDe(provedor);

        server.expect(requestTo("https://owm.local/geo/1.0/direct?q=Atibaia,BR&limit=5&appid=chave-teste"))
                .andRespond(withServerError());
        assertThrows(ProvedorIndisponivelException.class, () -> provedor.geocodificar("Atibaia"));

        server.reset();
        server.expect(requestTo("https://owm.local/geo/1.0/direct?q=Atibaia,BR&limit=5&appid=chave-teste"))
                .andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));
        assertThrows(ProvedorIndisponivelException.class, () -> provedor.geocodificar("Atibaia"));
    }

    @Test
    @DisplayName("previsaoDiaria converte daily com timezone_offset e ignora entradas sem dt")
    void deveConverterPrevisaoDiariaComOffset() {
        ProvedorOpenWeather provedor = new ProvedorOpenWeather("chave-teste", "https://owm.local", 2000);
        MockRestServiceServer server = servidorDe(provedor);

        server.expect(requestTo(
                        "https://owm.local/data/3.0/onecall?lat=-23.1&lon=-46.5&exclude=current,minutely,hourly,alerts&units=metric&lang=pt_br&appid=chave-teste"))
                .andRespond(withSuccess("""
                        {
                          "timezone_offset": -10800,
                          "daily": [
                            {"semDt": true},
                            {
                              "dt": 1767236400,
                              "temp": {"max": 29.5, "min": 18.2},
                              "weather": [{"id": 500, "description": "chuva leve"}],
                              "pop": 0.65,
                              "rain": 4.2
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<ProvedorPrevisao.DiaBruto> dias = provedor.previsaoDiaria(-23.1, -46.5);

        assertEquals(1, dias.size());
        ProvedorPrevisao.DiaBruto dia = dias.get(0);
        assertEquals(LocalDate.of(2026, 1, 1), dia.data());
        assertEquals("chuva leve", dia.resumo());
        assertEquals(500, dia.codigoTempo());
        assertEquals(29.5, dia.tempMax(), 0.01);
        assertEquals(18.2, dia.tempMin(), 0.01);
        assertEquals(0.65, dia.probabilidadeChuva(), 0.01);
        assertEquals(4.2, dia.chuvaMm(), 0.01);
        server.verify();
    }
}
