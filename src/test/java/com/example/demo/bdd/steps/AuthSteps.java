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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
        ctx.jwt = extrairToken(ctx.resposta);
    }

    @Quando("eu envio PUT \\/auth\\/alterar-senha com o JWT do login e o AlterarSenhaDTO:")
    public void putAlterarSenhaComJwt(DataTable tabela) throws Exception {
        enviarAlterarSenha(json(tabela), ctx.jwt);
    }

    @Quando("eu envio PUT \\/auth\\/alterar-senha sem JWT com o AlterarSenhaDTO:")
    public void putAlterarSenhaSemJwt(DataTable tabela) throws Exception {
        enviarAlterarSenha(json(tabela), null);
    }

    @Quando("eu envio PUT \\/auth\\/alterar-senha com JWT inválido e o AlterarSenhaDTO:")
    public void putAlterarSenhaJwtInvalido(DataTable tabela) throws Exception {
        enviarAlterarSenha(json(tabela), "token-invalido");
    }

    private void enviarAlterarSenha(String corpo, String jwt) throws Exception {
        var pedido = put("/auth/alterar-senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        if (jwt != null) {
            pedido.header("Authorization", "Bearer " + jwt);
        }
        MvcResult resultado = mockMvc.perform(pedido).andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }

    private String extrairToken(String json) {
        String marca = "\"token\":\"";
        int inicio = json.indexOf(marca);
        if (inicio < 0) {
            return null;
        }
        inicio += marca.length();
        int fim = json.indexOf('"', inicio);
        return fim < 0 ? null : json.substring(inicio, fim);
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
