package com.example.demo.controller;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.ServicoAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerEmpresa.class)
@AutoConfigureMockMvc(addFilters = false)
class ControllerEmpresaTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EmpresaRepository empresaRepository;
    @MockitoBean private ServicoAuth servicoAuth;
    @MockitoBean private com.example.demo.servico.previsao.ServicoPrevisaoTempo servicoPrevisaoTempo;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin-api-key", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void autenticarComoClienteDe(Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setRole(Role.CLIENTE);
        usuario.setEmpresa(empresa);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private void autenticarComTokenLegado() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cliente-token", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
    }

    private Empresa empresaDe(long id, String sessao) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setSessaoWhatsapp(sessao);
        empresa.setGoogleCalendarId("agenda-" + sessao + "@group.calendar.google.com");
        return empresa;
    }

    @Test
    @DisplayName("GET /empresas devolve coleção HAL para o painel")
    void deveListarEmpresasEmHal() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Recanto Vista Alegre");
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        when(empresaRepository.listarComModulos()).thenReturn(List.of(empresa));

        mockMvc.perform(get("/empresas").header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.empresas[0].nome").value("Recanto Vista Alegre"))
                .andExpect(jsonPath("$._embedded.empresas[0].sessaoWhatsapp").value("sessao_recanto_01"));
    }

    @Test
    @DisplayName("GET /empresas sem token administrativo retorna 401")
    void deveRecusarListagemSemToken() throws Exception {
        mockMvc.perform(get("/empresas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /empresas grava googleCalendarId, precoBase e módulos")
    void deveCadastrarComIdDaAgenda() throws Exception {
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(null);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> {
            Empresa empresa = invocacao.getArgument(0);
            empresa.setId(1L);
            return empresa;
        });

        mockMvc.perform(post("/empresas")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Recanto Vista Alegre",
                              "sessaoWhatsapp": "sessao_recanto_01",
                              "ramoDeAtuacao": "locacao",
                              "precoBase": 500.0,
                              "googleCalendarId": "agenda-cliente@group.calendar.google.com",
                              "email": "cliente@recanto.com",
                              "senha": "SenhaCliente123",
                              "modulosIniciais": ["IA_GEMINI", "VER_FOTOS"]
                            }
                            """))
                .andExpect(status().isCreated());

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(captor.capture());
        Empresa salva = captor.getValue();
        assertEquals("agenda-cliente@group.calendar.google.com", salva.getGoogleCalendarId());
        assertEquals("500.0", salva.getTabelaDePrecos());
        assertTrue(Boolean.TRUE.equals(salva.getUsaIA()));
        assertEquals(2, salva.getModulosAtivos().size());
        assertEquals("IA_GEMINI", salva.getModulosAtivos().get(0).getCodigoAcao());
        verify(servicoAuth).garantirEmailDisponivel("sessao_recanto_01@gamb.com.br");
        verify(servicoAuth).criarUsuarioCliente(salva, "sessao_recanto_01@gamb.com.br", "SenhaCliente123");
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin atualiza googleCalendarId e módulos")
    void deveAtualizarIdDaAgendaPeloAdmin() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "googleCalendarId": "nova-agenda@group.calendar.google.com",
                              "modulosAtivos": ["GOOGLE_CALENDAR"]
                            }
                            """))
                .andExpect(status().isOk());

        assertEquals("nova-agenda@group.calendar.google.com", empresa.getGoogleCalendarId());
        assertEquals(1, empresa.getModulosAtivos().size());
        assertEquals("GOOGLE_CALENDAR", empresa.getModulosAtivos().get(0).getCodigoAcao());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente persiste permiteReservaAutomatica")
    void deveAtualizarFlagDeReservaAutomaticaPeloCliente() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(false);
        autenticarComoClienteDe(empresa);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .header("x-cliente-token", "token-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Recanto Vista Alegre",
                              "permiteReservaAutomatica": true
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteReservaAutomatica").value(true));

        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente persiste regras, locação por hora e menu do bot")
    void deveAtualizarConfiguracoesDoRoboPeloCliente() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        autenticarComoClienteDe(empresa);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .header("x-cliente-token", "token-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Recanto Vista Alegre",
                              "locacaoPorHora": true,
                              "regrasLocacao": "Cancelar com 5 dias de antecedência",
                              "tabelaDePrecos": "Diária padrão: R$ 500,00",
                              "modulosMenu": [
                                {
                                  "codigoAcao": "VER_FOTOS",
                                  "textoMenu": "Ver as fotos do recanto",
                                  "ativo": true
                                },
                                {
                                  "codigoAcao": "MENU_CARDAPIO",
                                  "textoMenu": "Ver preços do espaço",
                                  "ativo": true
                                }
                              ]
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locacaoPorHora").value(true))
                .andExpect(jsonPath("$.regrasLocacao").value("Cancelar com 5 dias de antecedência"))
                .andExpect(jsonPath("$.modulosMenu[0].codigoAcao").value("VER_FOTOS"))
                .andExpect(jsonPath("$.modulosMenu[0].textoMenu").value("Ver as fotos do recanto"))
                .andExpect(jsonPath("$.modulosMenu[1].codigoAcao").value("MENU_CARDAPIO"))
                .andExpect(jsonPath("$.modulosMenu[1].textoMenu").value("Ver preços do espaço"));

        assertTrue(Boolean.TRUE.equals(empresa.getLocacaoPorHora()));
        assertEquals("Cancelar com 5 dias de antecedência", empresa.getRegrasLocacao());
        assertEquals("Diária padrão: R$ 500,00", empresa.getTabelaDePrecos());
        assertEquals(2, empresa.getModulosAtivos().size());
        assertEquals("VER_FOTOS", empresa.getModulosAtivos().get(0).getCodigoAcao());
        assertEquals("Ver as fotos do recanto", empresa.getModulosAtivos().get(0).getTextoMenu());
        assertTrue(Boolean.TRUE.equals(empresa.getModulosAtivos().get(0).getAtivo()));
        assertEquals("MENU_CARDAPIO", empresa.getModulosAtivos().get(1).getCodigoAcao());
        assertEquals("Ver preços do espaço", empresa.getModulosAtivos().get(1).getTextoMenu());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente persiste urlsGaleria, foto principal e linkGaleria")
    void deveAtualizarGaleriaPeloCliente() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        autenticarComoClienteDe(empresa);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .header("x-cliente-token", "token-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Recanto Vista Alegre",
                              "linkFotoPrincipal": "https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg",
                              "urlsGaleria": [
                                "https://res.cloudinary.com/demo/image/upload/v1/foto1.jpg",
                                "https://res.cloudinary.com/demo/image/upload/v1/foto2.jpg"
                              ],
                              "linkGaleria": "https://drive.google.com/drive/folders/exemplo"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkFotoPrincipal")
                        .value("https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg"))
                .andExpect(jsonPath("$.urlsGaleria[0]")
                        .value("https://res.cloudinary.com/demo/image/upload/v1/foto1.jpg"))
                .andExpect(jsonPath("$.urlsGaleria.length()").value(2))
                .andExpect(jsonPath("$.linkGaleria")
                        .value("https://drive.google.com/drive/folders/exemplo"));

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg",
                empresa.getLinkFotoPrincipal());
        assertEquals(2, empresa.getUrlsGaleria().size());
        assertEquals("https://drive.google.com/drive/folders/exemplo", empresa.getLinkGaleria());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/reset-senha sobrescreve a senha sem exigir a atual")
    void deveResetarSenhaPeloAdmin() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(empresa);

        mockMvc.perform(put("/empresas/sessao_recanto_01/reset-senha")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "novaSenha": "NovaSenha456" }
                            """))
                .andExpect(status().isNoContent());

        verify(servicoAuth).resetarSenhaDoCliente("sessao_recanto_01", "NovaSenha456");
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/reset-senha sem token administrativo retorna 401")
    void deveRecusarResetDeSenhaSemToken() throws Exception {
        mockMvc.perform(put("/empresas/sessao_recanto_01/reset-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "novaSenha": "NovaSenha456" }
                            """))
                .andExpect(status().isUnauthorized());

        verify(servicoAuth, never()).resetarSenhaDoCliente(any(), any());
    }

    @Test
    @DisplayName("GET /empresas/search/findBySessaoWhatsapp devolve cidade e interruptor da previsão sem coordenadas")
    void deveDevolverPrevisaoNoPainel() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        empresa.setCidadePrevisao("Atibaia");
        empresa.setCidadePrevisaoResolvida("Atibaia, São Paulo, BR");
        empresa.setPrevisaoTempoAtiva(true);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);

        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_recanto_01")
                        .header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidadePrevisao").value("Atibaia"))
                .andExpect(jsonPath("$.cidadePrevisaoResolvida").value("Atibaia, São Paulo, BR"))
                .andExpect(jsonPath("$.previsaoTempoAtiva").value(true))
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist());
    }

    private static final String CORPO_CIDADE = "{\"cidadePrevisao\":\"Atibaia\",\"previsaoTempoAtiva\":true}";

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente com token legado retorna 403 sem geocodificar, gravar nem gastar contador")
    void deveRecusarPutDoClienteComTokenLegado() throws Exception {
        Empresa alvo = empresaDe(10L, "sessao_alvo");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_alvo")).thenReturn(alvo);
        autenticarComTokenLegado();

        mockMvc.perform(put("/empresas/sessao_alvo/cliente")
                        .header("x-cliente-token", "x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CIDADE))
                .andExpect(status().isForbidden());

        verify(servicoPrevisaoTempo, never()).consumirTrocaDeCidade(anyLong());
        verify(servicoPrevisaoTempo, never()).geocodificar(anyString());
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente de cliente de outra empresa retorna 403 sem gastar contador nem cota")
    void deveRecusarPutDoClienteDeOutraEmpresa() throws Exception {
        Empresa alvo = empresaDe(10L, "sessao_alvo");
        Empresa intruso = empresaDe(20L, "sessao_intruso");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_alvo")).thenReturn(alvo);
        autenticarComoClienteDe(intruso);

        mockMvc.perform(put("/empresas/sessao_alvo/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CIDADE))
                .andExpect(status().isForbidden());

        verify(servicoPrevisaoTempo, never()).consumirTrocaDeCidade(anyLong());
        verify(servicoPrevisaoTempo, never()).geocodificar(anyString());
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente de sessão inexistente: não admin recebe 403 e admin recebe 404")
    void deveEsconderSessaoInexistenteNoPutDoCliente() throws Exception {
        when(empresaRepository.buscarPorSessaoComModulos("sessao_ausente")).thenReturn(null);

        autenticarComTokenLegado();
        mockMvc.perform(put("/empresas/sessao_ausente/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CIDADE))
                .andExpect(status().isForbidden());

        autenticarComoAdmin();
        mockMvc.perform(put("/empresas/sessao_ausente/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CIDADE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente: 503 do provedor estorna a troca e 400 de cidade não encontrada mantém a troca")
    void deveEstornarTrocaSoQuandoProvedorFalha() throws Exception {
        Empresa empresa = empresaDe(10L, "sessao_recanto_01");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(servicoPrevisaoTempo.consumirTrocaDeCidade(10L)).thenReturn(true);
        autenticarComoClienteDe(empresa);

        when(servicoPrevisaoTempo.geocodificar("Atibaia"))
                .thenThrow(new com.example.demo.servico.previsao.ProvedorIndisponivelException("fora"));
        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CIDADE))
                .andExpect(status().isServiceUnavailable());
        verify(servicoPrevisaoTempo).estornarTrocaDeCidade(10L);

        when(servicoPrevisaoTempo.geocodificar("Xyz1")).thenReturn(java.util.Optional.empty());
        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cidadePrevisao\":\"Xyz1\"}"))
                .andExpect(status().isBadRequest());
        verify(servicoPrevisaoTempo, org.mockito.Mockito.times(1)).estornarTrocaDeCidade(10L);
        verify(servicoPrevisaoTempo, org.mockito.Mockito.times(2)).consumirTrocaDeCidade(10L);
    }

    @Test
    @DisplayName("GET /empresas/search/findBySessaoWhatsapp: cliente de outra empresa e token legado recebem 403")
    void deveRecusarBuscaPorSessaoDeOutraEmpresa() throws Exception {
        Empresa alvo = empresaDe(10L, "sessao_alvo");
        Empresa intruso = empresaDe(20L, "sessao_intruso");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_alvo")).thenReturn(alvo);

        autenticarComoClienteDe(intruso);
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp").param("sessaoWhatsapp", "sessao_alvo"))
                .andExpect(status().isForbidden());

        autenticarComTokenLegado();
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_alvo")
                        .header("x-cliente-token", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /empresas/search/findBySessaoWhatsapp: googleCalendarId ausente para o cliente e presente para o admin")
    void deveEsconderGoogleCalendarIdDoCliente() throws Exception {
        Empresa empresa = empresaDe(10L, "sessao_recanto_01");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);

        autenticarComoClienteDe(empresa);
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp").param("sessaoWhatsapp", "sessao_recanto_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleCalendarId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist());

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_recanto_01")
                        .header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleCalendarId").value("agenda-sessao_recanto_01@group.calendar.google.com"));
    }

    @Test
    @DisplayName("GET /empresas/search/findBySessaoWhatsapp de sessão inexistente: não admin recebe 403 e admin recebe 404")
    void deveEsconderSessaoInexistenteNaBusca() throws Exception {
        when(empresaRepository.buscarPorSessaoComModulos("sessao_ausente")).thenReturn(null);

        autenticarComTokenLegado();
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_ausente")
                        .header("x-cliente-token", "x"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_ausente")
                        .header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /empresas/{sessao}/previsao: sessão inexistente e sessão de outra empresa dão a mesma resposta 403 para o cliente")
    void deveDarMesmaRespostaParaSessaoInexistenteEDeOutraEmpresa() throws Exception {
        Empresa alvo = empresaDe(10L, "sessao_alvo");
        Empresa intruso = empresaDe(20L, "sessao_intruso");
        when(empresaRepository.findBySessaoWhatsapp("sessao_alvo")).thenReturn(alvo);
        when(empresaRepository.findBySessaoWhatsapp("sessao_ausente")).thenReturn(null);
        autenticarComoClienteDe(intruso);

        String corpoOutra = mockMvc.perform(get("/empresas/sessao_alvo/previsao"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
        String corpoAusente = mockMvc.perform(get("/empresas/sessao_ausente/previsao"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
        assertEquals(corpoOutra, corpoAusente);

        autenticarComoAdmin();
        mockMvc.perform(get("/empresas/sessao_ausente/previsao")).andExpect(status().isNotFound());
        verify(servicoPrevisaoTempo, never()).consultar(any(Empresa.class));
    }
}
