package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class EmpresaSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private BddContexto ctx;

    @Dado("o token administrativo {string}")
    public void tokenAdministrativo(String token) {
        ctx.tokenAdmin = token;
    }

    @Dado("uma empresa persistida com sessão {string} e ramo {string}")
    public void empresaPersistida(String sessao, String ramo) {
        ctx.sessao = sessao;
        if (empresaRepository.findBySessaoWhatsapp(sessao) != null) {
            return;
        }
        Empresa empresa = new Empresa();
        empresa.setNome("Empresa " + sessao);
        empresa.setSessaoWhatsapp(sessao);
        empresa.setRamoDeAtuacao(ramo);
        empresa.setPermitePrevisaoTempo(true);
        empresaRepository.save(empresa);
    }

    @Quando("eu envio POST \\/empresas com o EmpresaCreateDTO:")
    public void postEmpresas(DataTable tabela) throws Exception {
        ctx.resposta = executar("POST", "/empresas", json(tabela), ctx.tokenAdmin, null);
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/cliente com o EmpresaUpdateDTO:")
    public void putCliente(String sessao, DataTable tabela) throws Exception {
        ctx.resposta = executar("PUT", "/empresas/" + sessao + "/cliente", json(tabela), "sua-chave-admin", null);
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/admin com o EmpresaUpdateAdminDTO:")
    public void putAdmin(String sessao, DataTable tabela) throws Exception {
        ctx.resposta = executar("PUT", "/empresas/" + sessao + "/admin", json(tabela), ctx.tokenAdmin, null);
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/reset-senha com o ResetSenhaAdminDTO:")
    public void putResetSenha(String sessao, DataTable tabela) throws Exception {
        ctx.resposta = executar("PUT", "/empresas/" + sessao + "/reset-senha", json(tabela), ctx.tokenAdmin, null);
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/admin sem token com o EmpresaUpdateAdminDTO:")
    public void putAdminSemToken(String sessao, DataTable tabela) throws Exception {
        ctx.tokenAdmin = null;
        ctx.resposta = executar("PUT", "/empresas/" + sessao + "/admin", json(tabela), null, null);
    }

    @Quando("eu envio DELETE \\/empresas\\/{word}")
    public void deleteEmpresa(String sessao) throws Exception {
        ctx.resposta = executar("DELETE", "/empresas/" + sessao, "", ctx.tokenAdmin, null);
    }

    @Então("o status HTTP deve ser {int}")
    public void statusHttp(int esperado) {
        assertThat(ctx.status).isEqualTo(esperado);
    }

    @E("o corpo deve seguir o schema EmpresaResponseDTO")
    public void corpoEmpresaResponse() {
        assertThat(ctx.resposta).contains("\"id\"");
        assertThat(ctx.resposta).contains("\"nome\"");
        assertThat(ctx.resposta).contains("\"sessaoWhatsapp\"");
        assertThat(ctx.resposta).contains("\"statusSessao\"");
        assertThat(ctx.resposta).doesNotContain("\"usaIA\"");
        assertThat(ctx.resposta).doesNotContain("\"modulosAtivos\"");
    }

    @E("o campo {string} deve ser {string}")
    public void campoDeveSer(String campo, String valor) {
        assertThat(ctx.resposta).contains("\"" + campo + "\":\"" + valor + "\"");
    }

    @E("o campo booleano {string} deve ser {word}")
    public void campoBooleanoDeveSer(String campo, String valor) {
        assertThat(ctx.resposta).contains("\"" + campo + "\":" + valor);
    }

    private String executar(String metodo, String path, String body, String adminToken, String clienteToken) throws Exception {
        var builder = switch (metodo) {
            case "POST" -> post(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException(metodo);
        };
        builder.contentType(MediaType.APPLICATION_JSON).content(body);
        if (adminToken != null) {
            builder.header("x-admin-token", adminToken);
        }
        if (clienteToken != null) {
            builder.header("x-cliente-token", clienteToken);
        }
        MvcResult resultado = mockMvc.perform(builder).andReturn();
        ctx.status = resultado.getResponse().getStatus();
        return resultado.getResponse().getContentAsString();
    }

    private String json(DataTable tabela) {
        Map<String, String> mapa = tabela.asMap();
        return mapa.entrySet().stream()
                .map(e -> {
                    String valor = e.getValue();
                    if ("true".equals(valor) || "false".equals(valor) || valor.matches("-?\\d+(\\.\\d+)?")) {
                        return "\"" + e.getKey() + "\":" + valor;
                    }
                    return "\"" + e.getKey() + "\":\"" + valor.replace("\"", "\\\"") + "\"";
                })
                .collect(Collectors.joining(",", "{", "}"));
    }
}
