package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class IaQuerySteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private BddContexto ctx;

    @Quando("eu envio POST \\/api\\/ia-query sem credencial")
    public void postSemCredencial() throws Exception {
        enviar("{\"query\":\"{ empresas { nome } }\"}", false);
    }

    @Quando("eu envio POST \\/api\\/ia-query com JSON malformado e o token administrativo")
    public void postJsonMalformado() throws Exception {
        enviar("{\"query\":", true);
    }

    private void enviar(String corpo, boolean comTokenAdmin) throws Exception {
        var pedido = post("/api/ia-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        if (comTokenAdmin) {
            pedido.header("x-admin-token", "sua-chave-admin");
        }
        MvcResult resultado = mockMvc.perform(pedido).andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }
}
