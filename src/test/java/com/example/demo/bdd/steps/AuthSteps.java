package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AuthSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private BddContexto ctx;

    @Quando("eu envio POST \\/auth\\/login com o LoginRequestDTO:")
    public void postLogin(DataTable tabela) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tabela)))
                .andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }

    @E("o corpo deve seguir o schema LoginResponseDTO")
    public void corpoLoginResponse() {
        assertThat(ctx.resposta).contains("\"token\"");
        assertThat(ctx.resposta).contains("\"email\"");
        assertThat(ctx.resposta).contains("\"role\"");
        assertThat(ctx.resposta).doesNotContain("\"senha\"");
    }

    private String json(DataTable tabela) {
        Map<String, String> mapa = tabela.asMap();
        return mapa.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue().replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }
}
