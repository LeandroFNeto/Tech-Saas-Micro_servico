package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import com.example.demo.servico.ServicoWppConnect;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class WhatsappSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private BddContexto ctx;
    @Autowired private ServicoWppConnect servicoWppConnect;

    @Quando("eu envio GET \\/whatsapp\\/{word}\\/status com o token administrativo")
    public void getStatusAdmin(String sessao) throws Exception {
        configurarMock(sessao);
        executar(get("/whatsapp/" + sessao + "/status").header("x-admin-token", ctx.tokenAdmin));
    }

    @Quando("eu envio GET \\/whatsapp\\/{word}\\/status com o JWT do login")
    public void getStatusJwt(String sessao) throws Exception {
        configurarMock(sessao);
        executar(get("/whatsapp/" + sessao + "/status").header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu envio GET \\/whatsapp\\/{word}\\/status com o token legado de cliente")
    public void getStatusLegado(String sessao) throws Exception {
        configurarMock(sessao);
        executar(get("/whatsapp/" + sessao + "/status").header("x-cliente-token", "token-legado"));
    }

    @Quando("eu envio GET \\/whatsapp\\/{word}\\/status sem token")
    public void getStatusSemToken(String sessao) throws Exception {
        configurarMock(sessao);
        executar(get("/whatsapp/" + sessao + "/status"));
    }

    @Quando("eu envio POST \\/whatsapp\\/{word}\\/iniciar com o token administrativo")
    public void postIniciarAdmin(String sessao) throws Exception {
        configurarMock(sessao);
        executar(post("/whatsapp/" + sessao + "/iniciar").header("x-admin-token", ctx.tokenAdmin));
    }

    @Quando("eu envio POST \\/whatsapp\\/{word}\\/iniciar com o JWT do login")
    public void postIniciarJwt(String sessao) throws Exception {
        configurarMock(sessao);
        executar(post("/whatsapp/" + sessao + "/iniciar").header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu envio POST \\/whatsapp\\/{word}\\/iniciar com o token legado de cliente")
    public void postIniciarLegado(String sessao) throws Exception {
        configurarMock(sessao);
        executar(post("/whatsapp/" + sessao + "/iniciar").header("x-cliente-token", "token-legado"));
    }

    @Quando("eu envio POST \\/whatsapp\\/{word}\\/iniciar sem token")
    public void postIniciarSemToken(String sessao) throws Exception {
        configurarMock(sessao);
        executar(post("/whatsapp/" + sessao + "/iniciar"));
    }

    private void configurarMock(String sessao) {
        when(servicoWppConnect.consultarStatus(anyString()))
                .thenReturn(new SessaoStatusResponseDTO(sessao, "CONNECTED", null));
        when(servicoWppConnect.iniciarSessao(anyString()))
                .thenReturn(new SessaoStatusResponseDTO(sessao, "CONNECTED", null));
    }

    private void executar(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult resultado = mockMvc.perform(builder).andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }
}
