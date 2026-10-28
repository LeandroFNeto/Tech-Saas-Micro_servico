package com.example.demo.bdd.steps;

import com.example.demo.bdd.BddContexto;
import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.previsao.ProvedorIndisponivelException;
import com.example.demo.servico.previsao.ProvedorPrevisao;
import com.example.demo.servico.previsao.ServicoPrevisaoTempo;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class PrevisaoSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ProvedorPrevisao provedorPrevisao;
    @Autowired private BddContexto ctx;

    @Dado("o provedor de previsão reconhece qualquer cidade")
    public void provedorReconhece() {
        doReturn(Optional.of(new ProvedorPrevisao.Localizacao(-23.11, -46.55, "Atibaia, São Paulo, BR")))
                .when(provedorPrevisao).geocodificar(anyString());
    }

    @Dado("o provedor de previsão não reconhece a cidade")
    public void provedorNaoReconhece() {
        doReturn(Optional.empty()).when(provedorPrevisao).geocodificar(anyString());
    }

    @Dado("o provedor de previsão está fora do ar")
    public void provedorForaDoAr() {
        doThrow(new ProvedorIndisponivelException("fora")).when(provedorPrevisao).geocodificar(anyString());
    }

    @Dado("o provedor de previsão devolve hoje com chuva forte")
    public void provedorDevolveChuvaForte() {
        LocalDate hoje = LocalDate.now(ServicoPrevisaoTempo.FUSO);
        when(provedorPrevisao.previsaoDiaria(anyDouble(), anyDouble())).thenReturn(List.of(
                new ProvedorPrevisao.DiaBruto(hoje, "chuva forte", 502, 24.0, 18.0, 0.9, 12.0)));
    }

    @Quando("a empresa {string} troca a cidade da previsão {int} vezes")
    public void trocaCidade(String sessao, int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            enviarCidade(sessao, "Cidade " + i);
        }
    }

    @Quando("a empresa {string} troca a cidade da previsão para {string}")
    public void trocaCidadePara(String sessao, String cidade) throws Exception {
        enviarCidade(sessao, cidade);
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/previsao com o token administrativo")
    public void getPrevisaoAdmin(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/previsao").header("x-admin-token", ctx.tokenAdmin));
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/previsao com o JWT do login")
    public void getPrevisaoJwt(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/previsao").header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/previsao com o token legado de cliente")
    public void getPrevisaoLegado(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/previsao").header("x-cliente-token", "token-cliente"));
    }

    @Quando("eu envio GET \\/empresas\\/{word}\\/previsao sem token")
    public void getPrevisaoSemToken(String sessao) throws Exception {
        executar(get("/empresas/" + sessao + "/previsao"));
    }

    @Dado("o provedor de previsão ainda não foi consultado")
    public void provedorNaoConsultadoAinda() {
        clearInvocations(provedorPrevisao);
    }

    @E("o provedor de previsão não deve ter sido consultado")
    public void provedorNaoConsultado() {
        verify(provedorPrevisao, never()).geocodificar(anyString());
    }

    @E("o corpo não deve conter {string}")
    public void corpoNaoDeveConter(String trecho) {
        assertThat(ctx.resposta).doesNotContain(trecho);
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/cliente com a cidade {string} e o token legado de cliente")
    public void putCidadeLegado(String sessao, String cidade) throws Exception {
        executar(put("/empresas/" + sessao + "/cliente")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cidadePrevisao\":\"" + cidade + "\",\"previsaoTempoAtiva\":true}")
                .header("x-cliente-token", "token-cliente"));
    }

    @Quando("eu envio PUT \\/empresas\\/{word}\\/cliente com a cidade {string} e o JWT do login")
    public void putCidadeJwt(String sessao, String cidade) throws Exception {
        executar(put("/empresas/" + sessao + "/cliente")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cidadePrevisao\":\"" + cidade + "\",\"previsaoTempoAtiva\":true}")
                .header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu busco a empresa {word} pelo painel com o token legado de cliente")
    public void buscarPainelLegado(String sessao) throws Exception {
        executar(get("/empresas/search/findBySessaoWhatsapp").param("sessaoWhatsapp", sessao)
                .header("x-cliente-token", "token-cliente"));
    }

    @Quando("eu busco a empresa {word} pelo painel com o JWT do login")
    public void buscarPainelJwt(String sessao) throws Exception {
        executar(get("/empresas/search/findBySessaoWhatsapp").param("sessaoWhatsapp", sessao)
                .header("Authorization", "Bearer " + ctx.jwt));
    }

    @Quando("eu busco a empresa {word} pelo painel com o token administrativo")
    public void buscarPainelAdmin(String sessao) throws Exception {
        executar(get("/empresas/search/findBySessaoWhatsapp").param("sessaoWhatsapp", sessao)
                .header("x-admin-token", ctx.tokenAdmin));
    }

    @E("a resposta guardada como {string}")
    public void guardarResposta(String chave) {
        respostasGuardadas.put(chave, ctx.status + "|" + ctx.resposta);
    }

    @E("a resposta atual deve ser igual à guardada como {string}")
    public void respostaIgualGuardada(String chave) {
        assertThat(ctx.status + "|" + ctx.resposta).isEqualTo(respostasGuardadas.get(chave));
    }

    private final java.util.Map<String, String> respostasGuardadas = new java.util.HashMap<>();

    @E("o corpo deve conter {string}")
    public void corpoDeveConter(String trecho) {
        assertThat(ctx.resposta).contains(trecho);
    }

    @E("a empresa {string} não deve ter cidade de previsão gravada")
    public void semCidadeGravada(String sessao) {
        Empresa empresa = empresaRepository.findBySessaoWhatsapp(sessao);
        assertThat(empresa.getCidadePrevisao()).isNull();
        assertThat(empresa.getLatitude()).isNull();
        assertThat(empresa.getPrevisaoTempoAtiva()).isNotEqualTo(Boolean.TRUE);
    }

    @E("a empresa {string} deve ter a cidade de previsão {string} gravada")
    public void cidadeGravada(String sessao, String cidade) {
        Empresa empresa = empresaRepository.findBySessaoWhatsapp(sessao);
        assertThat(empresa.getCidadePrevisao()).isEqualTo(cidade);
        assertThat(empresa.getLatitude()).isNotNull();
    }

    private void enviarCidade(String sessao, String cidade) throws Exception {
        String corpo = "{\"cidadePrevisao\":\"" + cidade + "\",\"previsaoTempoAtiva\":true}";
        executar(put("/empresas/" + sessao + "/cliente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo)
                .header("x-admin-token", "sua-chave-admin"));
    }

    private void executar(MockHttpServletRequestBuilder pedido) throws Exception {
        MvcResult resultado = mockMvc.perform(pedido).andReturn();
        ctx.status = resultado.getResponse().getStatus();
        ctx.resposta = resultado.getResponse().getContentAsString();
    }
}
