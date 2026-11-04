package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.strategy.FactoryModulo;
import com.example.demo.strategy.LocacaoStrategy;
import com.example.demo.strategy.ModuloAtendimentoStrategy;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class RoteamentoSteps {

    @Autowired private FactoryModulo factoryModulo;
    @Autowired private BddContexto ctx;

    private ModuloAtendimentoStrategy strategy;

    @Dado("as Strategies carregadas no contexto Spring")
    public void strategiesCarregadas() {
        assertThat(factoryModulo).isNotNull();
    }

    @Quando("a FactoryModulo busca o ramo {string}")
    public void factoryBusca(String ramo) {
        try {
            strategy = factoryModulo.obterEstrategia(ramo);
            ctx.erroFactory = null;
        } catch (Exception e) {
            ctx.erroFactory = e;
            strategy = null;
        }
    }

    @Então("a Strategy retornada deve ser LocacaoStrategy")
    public void deveSerLocacao() {
        assertThat(ctx.erroFactory).isNull();
        assertThat(strategy).isInstanceOf(LocacaoStrategy.class);
        assertThat(strategy.getRamoDeAtuacao()).isEqualTo("LOCACAO");
    }

    @Então("a Strategy retornada deve ser a {string} do ramo {string}")
    public void deveSerAStrategyDoRamo(String classe, String ramo) {
        assertThat(ctx.erroFactory).isNull();
        assertThat(strategy.getClass().getSimpleName()).isEqualTo(classe);
        assertThat(strategy.getRamoDeAtuacao()).isEqualTo(ramo);
    }

    @Então("deve ocorrer erro de ramo não suportado")
    public void ramoNaoSuportado() {
        assertThat(ctx.erroFactory)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ramo de atuação não suportado");
    }
}
