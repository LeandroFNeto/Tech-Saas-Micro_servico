package com.example.demo.servico.previsao;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ProvedorOpenWeather implements ProvedorPrevisao {

    private final RestTemplate restTemplate;
    private final String chave;
    private final String urlBase;

    public ProvedorOpenWeather(
            @Value("${previsao.openweather.chave:}") String chave,
            @Value("${previsao.openweather.url-base:https://api.openweathermap.org}") String urlBase,
            @Value("${previsao.openweather.timeout-ms:3000}") int timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeoutMs);
        fabrica.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(fabrica);
        this.chave = chave == null ? "" : chave.trim();
        this.urlBase = urlBase;
    }

    @Override
    public Optional<Localizacao> geocodificar(String cidade) {
        URI uri = UriComponentsBuilder.fromUriString(urlBase)
                .path("/geo/1.0/direct")
                .queryParam("q", cidade + ",BR")
                .queryParam("limit", 5)
                .queryParam("appid", chaveObrigatoria())
                .build()
                .encode()
                .toUri();
        JsonNode resposta = chamar(uri);
        if (resposta == null || !resposta.isArray()) {
            throw new ProvedorIndisponivelException("Resposta inválida do provedor");
        }
        for (JsonNode item : resposta) {
            if (!"BR".equalsIgnoreCase(item.path("country").asText())) {
                continue;
            }
            if (!item.hasNonNull("lat") || !item.hasNonNull("lon")) {
                continue;
            }
            return Optional.of(new Localizacao(
                    item.get("lat").asDouble(),
                    item.get("lon").asDouble(),
                    nomeResolvido(item)));
        }
        return Optional.empty();
    }

    @Override
    public List<DiaBruto> previsaoDiaria(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(urlBase)
                .path("/data/3.0/onecall")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("exclude", "current,minutely,hourly,alerts")
                .queryParam("units", "metric")
                .queryParam("lang", "pt_br")
                .queryParam("appid", chaveObrigatoria())
                .build()
                .encode()
                .toUri();
        JsonNode resposta = chamar(uri);
        if (resposta == null || !resposta.path("daily").isArray()) {
            throw new ProvedorIndisponivelException("Resposta inválida do provedor");
        }
        long deslocamento = resposta.path("timezone_offset").asLong(0);
        List<DiaBruto> dias = new ArrayList<>();
        for (JsonNode dia : resposta.get("daily")) {
            if (!dia.hasNonNull("dt")) {
                continue;
            }
            JsonNode tempo = dia.path("weather").path(0);
            dias.add(new DiaBruto(
                    Instant.ofEpochSecond(dia.get("dt").asLong() + deslocamento).atZone(ZoneOffset.UTC).toLocalDate(),
                    tempo.path("description").asText(""),
                    tempo.path("id").asInt(0),
                    dia.path("temp").path("max").asDouble(),
                    dia.path("temp").path("min").asDouble(),
                    dia.path("pop").asDouble(0),
                    dia.path("rain").asDouble(0)));
        }
        return dias;
    }

    private String chaveObrigatoria() {
        if (chave.isEmpty()) {
            throw new ProvedorIndisponivelException("Chave da API de previsão ausente");
        }
        return chave;
    }

    private JsonNode chamar(URI uri) {
        try {
            return restTemplate.getForObject(uri, JsonNode.class);
        } catch (Exception falha) {
            throw new ProvedorIndisponivelException("Provedor de previsão indisponível");
        }
    }

    private String nomeResolvido(JsonNode item) {
        StringBuilder nome = new StringBuilder(item.path("name").asText(""));
        String estado = item.path("state").asText("");
        if (!estado.isBlank()) {
            nome.append(", ").append(estado);
        }
        nome.append(", ").append(item.path("country").asText(""));
        return nome.toString();
    }
}
