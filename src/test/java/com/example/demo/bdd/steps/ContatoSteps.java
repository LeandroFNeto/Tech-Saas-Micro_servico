package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

public class ContatoSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private BddContexto ctx;

    @Quando("eu envio GET \\/empresas\\/{word}\\/contatos com o JWT do login")
    public void getContatosJwt(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/contatos").header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/contatos sem token")
    public void getContatosSemToken(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/contatos"));
    }

    @Quando("eu envio POST \\/empresas\\/{word}\\/contatos\\/{long}\\/numero com o JWT do login")
    public void postRevelarNumeroJwt(String sessao, Long id) throws Exception {
        executar(post("/empresas/" + sessao + "/contatos/" + id + "/numero").header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu envio DELETE \\/empresas\\/{word}\\/contatos\\/{long} com o JWT do login")
    public void deleteContatoJwt(String sessao, Long id) throws Exception {
        executar(delete("/empresas/" + sessao + "/contatos/" + id).header("Authorization", "Bearer " + ctx.jwt));
    }

    private void executar(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult res = mockMvc.perform(builder).andReturn();
        ctx.status = res.getResponse().getStatus();
        ctx.resposta = res.getResponse().getContentAsString();
    }
}
