package com.example.demo.strategy;

import com.example.demo.model.Empresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactoryModuloTest {

    private FactoryModulo factory;
    private final ModuloAtendimentoStrategy locacao = new StubLocacao();

    @BeforeEach
    void setUp() {
        factory = new FactoryModulo(List.of(locacao));
    }

    @Test
    @DisplayName("Resolve LOCACAO, locacao e Locação para a mesma Strategy")
    void deveResolverVariantesDeLocacao() {
        assertThat(factory.obterEstrategia("LOCACAO")).isSameAs(locacao);
        assertThat(factory.obterEstrategia("locacao")).isSameAs(locacao);
        assertThat(factory.obterEstrategia("Locação")).isSameAs(locacao);
        assertThat(factory.obterEstrategia(" locacao ")).isSameAs(locacao);
    }

    @Test
    @DisplayName("Ramo desconhecido continua rejeitado")
    void deveRejeitarRamoDesconhecido() {
        assertThatThrownBy(() -> factory.obterEstrategia("CLINICA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ramo de atuação não suportado")
                .hasMessageContaining("CLINICA");
    }

    private static final class StubLocacao implements ModuloAtendimentoStrategy {
        @Override
        public void processarMensagem(Empresa empresa, String numeroCliente, String textoRecebido, String estadoAtual) {
        }

        @Override
        public String getRamoDeAtuacao() {
            return "LOCACAO";
        }
    }
}
