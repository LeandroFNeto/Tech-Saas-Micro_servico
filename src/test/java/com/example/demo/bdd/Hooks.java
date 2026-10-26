package com.example.demo.bdd;

import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.strategy.FactoryModulo;
import io.cucumber.java.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class Hooks {

    @Autowired FactoryModulo factoryModulo;
    @Autowired Observadorwhatsapp observadorwhatsapp;
    @Autowired ServicoMensagem servicoMensagem;
    @Autowired ServicoIA servicoIA;
    @Autowired ServicoReserva servicoReserva;
    @Autowired BddContexto ctx;

    @Before
    public void limparCenario() {
        Mockito.clearInvocations(factoryModulo, observadorwhatsapp);
        Mockito.reset(servicoMensagem, servicoIA, servicoReserva);
        ctx.tokenAdmin = null;
        ctx.jwt = null;
        ctx.status = 0;
        ctx.resposta = null;
        ctx.erroFactory = null;
    }
}
