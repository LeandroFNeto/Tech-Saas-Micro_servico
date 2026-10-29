package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.servico.ServicoGoogleagenda;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class FuncionamentoSteps {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private BddContexto ctx;
    @Autowired private ServicoGoogleagenda servicoGoogleagenda;

    private String idDiaEspecial;

    @Quando("eu envio GET \\/empresas\\/{word}\\/funcionamento com o token administrativo")
    public void getComAdmin(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/funcionamento").header("x-admin-token", ctx.tokenAdmin));
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/funcionamento sem token")
    public void getSemToken(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/funcionamento"));
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/funcionamento com o JWT do login")
    public void getComJwt(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/funcionamento").header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/funcionamento com o corpo:")
    public void putFuncionamento(String sessao, String corpo) throws Exception {
        executar(put("/empresas/" + sessao + "/funcionamento")
                .header("x-admin-token", ctx.tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Dado("a agenda tem o evento {string} em qualquer data")
    public void agendaTemEvento(String resumo) throws Exception {
        when(servicoGoogleagenda.resumosDoDia(anyString(), any())).thenReturn(List.of(resumo));
    }

    @Quando("eu cadastro em {word} o dia especial fechado daqui a {int} dias com o motivo {string}")
    public void cadastrarFechado(String sessao, int dias, String motivo) throws Exception {
        String corpo = "{\"data\":\"" + daquiADias(dias) + "\",\"fechado\":true,\"motivo\":\"" + motivo + "\"}";
        cadastrar(sessao, corpo);
    }

    @Quando("eu cadastro em {word} o dia especial daqui a {int} dias das {string} às {string}")
    public void cadastrarComHorario(String sessao, int dias, String abre, String fecha) throws Exception {
        String corpo = "{\"data\":\"" + daquiADias(dias) + "\",\"fechado\":false,\"abre\":\"" + abre
                + "\",\"fecha\":\"" + fecha + "\",\"motivo\":\"Horário estendido\"}";
        cadastrar(sessao, corpo);
    }

    @Quando("eu removo em {word} o dia especial guardado")
    public void removerGuardado(String sessao) throws Exception {
        assertThat(idDiaEspecial).as("dia especial guardado").isNotNull();
        executar(delete("/empresas/" + sessao + "/dias-especiais/" + idDiaEspecial)
                .header("x-admin-token", ctx.tokenAdmin));
    }

    private void cadastrar(String sessao, String corpo) throws Exception {
        executar(post("/empresas/" + sessao + "/dias-especiais")
                .header("x-admin-token", ctx.tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
        Matcher id = ID.matcher(ctx.resposta);
        if (ctx.status == 201 && id.find()) {
            idDiaEspecial = id.group(1);
        }
    }

    private void executar(MockHttpServletRequestBuilder requisicao) throws Exception {
        MvcResult resultado = mockMvc.perform(requisicao).andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }

    private String daquiADias(int dias) {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(dias).toString();
    }
}
