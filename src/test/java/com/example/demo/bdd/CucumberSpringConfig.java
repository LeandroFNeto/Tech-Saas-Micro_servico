package com.example.demo.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.demo.servico.ServicoGoogleagenda;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.servico.ServicoReserva;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.strategy.FactoryModulo;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfig {

    // Dublê do WPPConnect: os cenários BDD exercitam o fluxo sem chamar a rede.
    @MockitoBean
    ServicoMensagem servicoMensagem;

    // Dublê da Gemini: o teste descreve o comportamento, não a API de IA real.
    @MockitoBean
    ServicoIA servicoIA;

    // Isola a reserva da infraestrutura; o cenário controla o que o serviço responde.
    @MockitoBean
    ServicoReserva servicoReserva;

    // Evita a necessidade do arquivo crendecial.json ao subir o contexto Cucumber.
    @MockitoBean
    ServicoGoogleagenda servicoGoogleagenda;

    @MockitoBean
    com.example.demo.servico.previsao.ProvedorPrevisao provedorPrevisao;

    @MockitoSpyBean
    FactoryModulo factoryModulo;

    @MockitoSpyBean
    Observadorwhatsapp observadorwhatsapp;
}
