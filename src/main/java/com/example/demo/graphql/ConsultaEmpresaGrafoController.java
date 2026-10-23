package com.example.demo.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Endpoint GraphQL em /api/ia-query. Não publica rota REST nova de empresa.
 */
@Controller
public class ConsultaEmpresaGrafoController {

    private final ConsultaEmpresaGrafo consulta;

    public ConsultaEmpresaGrafoController(ConsultaEmpresaGrafo consulta) {
        this.consulta = consulta;
    }

    @QueryMapping
    public EmpresaConsulta empresa(@Argument String id) {
        return consulta.buscarPorId(id);
    }

    @QueryMapping
    public List<EmpresaConsulta> empresas() {
        return consulta.listar();
    }

    @QueryMapping
    public EmpresaConsulta empresaPorSessao(@Argument String sessaoWhatsapp) {
        return consulta.buscarPorSessao(sessaoWhatsapp);
    }
}
