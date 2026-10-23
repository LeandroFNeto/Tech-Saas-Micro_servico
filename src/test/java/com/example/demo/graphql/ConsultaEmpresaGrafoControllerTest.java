package com.example.demo.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@GraphQlTest(ConsultaEmpresaGrafoController.class)
class ConsultaEmpresaGrafoControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ConsultaEmpresaGrafo consulta;

    @Test
    void devolveSoNomeEPrecoQuandoAQueryPedeEssesCampos() {
        when(consulta.listar()).thenReturn(List.of(new EmpresaConsulta(
                "1",
                "Recanto Vista Alegre",
                "Diária R$ 500",
                "Lazer",
                "Cancelar com 5 dias",
                "Olá",
                "sessao_recanto_01",
                "https://maps.example",
                "https://cdn/hero.jpg",
                "https://drive.example",
                List.of("https://cdn/1.jpg"),
                true,
                false,
                false
        )));

        Map<String, Object> empresa = graphQlTester.document("{ empresas { nome tabelaDePrecos } }")
                .execute()
                .errors()
                .verify()
                .path("empresas[0]")
                .entity(new ParameterizedTypeReference<Map<String, Object>>() {})
                .get();

        assertEquals(Set.of("nome", "tabelaDePrecos"), empresa.keySet());
        assertEquals("Recanto Vista Alegre", empresa.get("nome"));
        assertEquals("Diária R$ 500", empresa.get("tabelaDePrecos"));
    }
}
