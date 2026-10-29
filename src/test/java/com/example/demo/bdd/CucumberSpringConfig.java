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
import com.example.demo.servico.ServicoWppConnect;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.strategy.FactoryModulo;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfig {

    @MockitoBean
    ServicoMensagem servicoMensagem;

    @MockitoBean
    ServicoIA servicoIA;

    @MockitoBean
    ServicoReserva servicoReserva;

    @MockitoBean
    ServicoGoogleagenda servicoGoogleagenda;

    @MockitoBean
    com.example.demo.servico.previsao.ProvedorPrevisao provedorPrevisao;

    @MockitoBean
    ServicoWppConnect servicoWppConnect;

    @MockitoSpyBean
    FactoryModulo factoryModulo;

    @MockitoSpyBean
    Observadorwhatsapp observadorwhatsapp;
}
