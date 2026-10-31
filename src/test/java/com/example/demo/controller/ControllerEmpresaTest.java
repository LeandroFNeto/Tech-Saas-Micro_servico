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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerEmpresa.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import(com.example.demo.servico.ServicoAcessoEmpresa.class)
class ControllerEmpresaTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EmpresaRepository empresaRepository;
    @MockitoBean private ServicoAuth servicoAuth;
    @MockitoBean private com.example.demo.servico.previsao.ServicoPrevisaoTempo servicoPrevisaoTempo;
    @MockitoBean private com.example.demo.servico.ServicoWppConnect servicoWppConnect;

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

    private void autenticarComoRole(Role role) {
        Usuario usuario = new Usuario();
        usuario.setRole(role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
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

    private void exigirMensagem(org.springframework.test.web.servlet.MvcResult resultado, String trecho) {
        assertTrue(resultado.getResolvedException().getMessage().contains(trecho));
    }

    private static String corpoPost(String extras) {
        return "{\"nome\":\"Recanto Vista Alegre\",\"sessaoWhatsapp\":\"sessao_recanto_01\","
                + "\"ramoDeAtuacao\":\"locacao\",\"email\":\"cliente@recanto.com\","
                + "\"senha\":\"SenhaCliente123\"" + extras + "}";
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente com valor diferente de permiteReservaAutomatica retorna 400 e não grava")
    void deveRecusarMudancaDeReservaAutomaticaPeloCliente() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(false);
        autenticarComoClienteDe(empresa);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                                                .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Recanto Vista Alegre",
                              "permiteReservaAutomatica": true
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(resultado -> exigirMensagem(resultado, "A reserva automática é liberada pelo administrador."));

        verify(empresaRepository, never()).save(any(Empresa.class));
        assertEquals(false, empresa.getPermiteReservaAutomatica());
        assertEquals(null, empresa.getNome());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente com permiteReservaAutomatica igual ao atual é aceito e ignorado")
    void deveAceitarValorIgualDeReservaAutomaticaPeloCliente() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        autenticarComoClienteDe(empresa);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
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
        assertEquals("Recanto Vista Alegre", empresa.getNome());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente sem permiteReservaAutomatica mantém o valor atual")
    void deveManterReservaAutomaticaQuandoClienteNaoEnviaOCampo() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        autenticarComoClienteDe(empresa);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                                                .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Recanto Vista Alegre"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteReservaAutomatica").value(true));

        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("POST /empresas ligando a reserva automática sem agenda retorna 400 e não grava")
    void deveRecusarCadastroComReservaAutomaticaSemAgenda() throws Exception {
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(null);

        mockMvc.perform(post("/empresas")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPost(",\"permiteReservaAutomatica\":true")))
                .andExpect(status().isBadRequest())
                .andExpect(resultado -> exigirMensagem(resultado, "Cadastre a agenda do Google antes de ligar a reserva automática."));

        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("POST /empresas ligando a reserva automática com agenda em branco retorna 400")
    void deveRecusarCadastroComReservaAutomaticaEAgendaEmBranco() throws Exception {
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(null);

        mockMvc.perform(post("/empresas")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPost(",\"permiteReservaAutomatica\":true,\"googleCalendarId\":\"   \"")))
                .andExpect(status().isBadRequest());

        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("POST /empresas ligando a reserva automática com agenda grava a flag")
    void deveCadastrarComReservaAutomaticaEAgenda() throws Exception {
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(null);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> {
            Empresa empresa = invocacao.getArgument(0);
            empresa.setId(1L);
            return empresa;
        });

        mockMvc.perform(post("/empresas")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPost(",\"permiteReservaAutomatica\":true,\"googleCalendarId\":\"agenda@group.calendar.google.com\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteReservaAutomatica").value(true));

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(captor.capture());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getPermiteReservaAutomatica()));
        assertEquals("agenda@group.calendar.google.com", captor.getValue().getGoogleCalendarId());
    }

    @Test
    @DisplayName("POST /empresas sem agenda e sem ligar a reserva automática funciona")
    void deveCadastrarSemAgendaQuandoReservaAutomaticaDesligada() throws Exception {
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(null);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> {
            Empresa empresa = invocacao.getArgument(0);
            empresa.setId(1L);
            return empresa;
        });

        mockMvc.perform(post("/empresas")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPost("")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteReservaAutomatica").value(false));

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(captor.capture());
        assertEquals(false, Boolean.TRUE.equals(captor.getValue().getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin ligando a reserva automática sem agenda retorna 400 e não grava")
    void deveRecusarLigarReservaAutomaticaSemAgendaPeloAdmin() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(resultado -> exigirMensagem(resultado, "Cadastre a agenda do Google antes de ligar a reserva automática."));

        verify(empresaRepository, never()).save(any(Empresa.class));
        assertEquals(false, empresa.getPermiteReservaAutomatica());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin ligando e apagando a agenda no mesmo pedido retorna 400")
    void deveRecusarLigarReservaAutomaticaApagandoAAgenda() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true,
                              "googleCalendarId": ""
                            }
                            """))
                .andExpect(status().isBadRequest());

        verify(empresaRepository, never()).save(any(Empresa.class));
        assertEquals("agenda-sessao_recanto_01@group.calendar.google.com", empresa.getGoogleCalendarId());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin ligando com agenda já cadastrada grava a flag")
    void deveLigarReservaAutomaticaComAgendaJaCadastrada() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteReservaAutomatica").value(true));

        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin ligando junto com uma agenda nova grava as duas")
    void deveLigarReservaAutomaticaJuntoComAgendaNova() throws Exception {
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
                              "permiteReservaAutomatica": true,
                              "googleCalendarId": "nova@group.calendar.google.com"
                            }
                            """))
                .andExpect(status().isOk());

        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
        assertEquals("nova@group.calendar.google.com", empresa.getGoogleCalendarId());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin apaga a agenda de empresa ligada sem erro")
    void devePermitirApagarAgendaDeEmpresaComReservaAutomaticaLigada() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "googleCalendarId": ""
                            }
                            """))
                .andExpect(status().isOk());

        assertEquals(null, empresa.getGoogleCalendarId());
        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin com true em empresa já ligada e sem agenda retorna 200")
    void deveAceitarTrueEmEmpresaJaLigadaSemAgenda() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        empresa.setGoogleCalendarId(null);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true
                            }
                            """))
                .andExpect(status().isOk());

        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin apaga a agenda de empresa ligada mesmo mandando true")
    void deveApagarAgendaDeEmpresaLigadaMandandoTrue() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true,
                              "googleCalendarId": ""
                            }
                            """))
                .andExpect(status().isOk());

        assertEquals(null, empresa.getGoogleCalendarId());
        assertTrue(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin edita a agenda de empresa ligada mandando true")
    void deveEditarAgendaDeEmpresaLigadaSemAgendaMandandoTrue() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true,
                              "googleCalendarId": "outra@group.calendar.google.com"
                            }
                            """))
                .andExpect(status().isOk());

        assertEquals("outra@group.calendar.google.com", empresa.getGoogleCalendarId());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin de desligada para ligada com agenda vazia retorna 400")
    void deveRecusarDesligadaParaLigadaSemAgendaFinal() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(false);
        empresa.setGoogleCalendarId(null);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": true
                            }
                            """))
                .andExpect(status().isBadRequest());

        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/admin desligando a reserva automática sem agenda é aceito")
    void deveAceitarDesligarReservaAutomaticaSemAgenda() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermiteReservaAutomatica(true);
        empresa.setGoogleCalendarId(null);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/empresas/sessao_recanto_01/admin")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "permiteReservaAutomatica": false
                            }
                            """))
                .andExpect(status().isOk());

        assertEquals(false, empresa.getPermiteReservaAutomatica());
    }

    @Test
    @DisplayName("POST /empresas com agenda e sem o campo devolve permiteReservaAutomatica=false")
    void deveNascerDesligadaQuandoPostNaoEnviaOCampo() throws Exception {
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(null);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacao -> {
            Empresa empresa = invocacao.getArgument(0);
            empresa.setId(1L);
            return empresa;
        });

        mockMvc.perform(post("/empresas")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPost(",\"googleCalendarId\":\"agenda@group.calendar.google.com\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteReservaAutomatica").value(false));
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
        mockMvc.perform(put("/empresas/sessao_alvo/cliente")
                        .header("x-cliente-token", "x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CIDADE))
                .andExpect(status().isUnauthorized());

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

        Empresa intruso = empresaDe(20L, "sessao_intruso");
        autenticarComoClienteDe(intruso);
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

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_alvo")
                        .header("x-cliente-token", "x"))
                .andExpect(status().isUnauthorized());
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

        Empresa intruso = empresaDe(20L, "sessao_intruso");
        autenticarComoClienteDe(intruso);
        mockMvc.perform(get("/empresas/search/findBySessaoWhatsapp")
                        .param("sessaoWhatsapp", "sessao_ausente"))
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

    @Test
    @DisplayName("GET /empresas/{sessao}/previsao com x-cliente-token sozinho retorna 401")
    void deveRecusarPrevisaoComXClienteTokenSozinho() throws Exception {
        mockMvc.perform(get("/empresas/sessao_recanto_01/previsao")
                        .header("x-cliente-token", "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /empresas/{sessao}: admin desativa empresa com soft delete e desconecta WhatsApp")
    void deveDesativarEmpresaComSucesso() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setAtivo(true);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(empresa);
        autenticarComoAdmin();

        mockMvc.perform(delete("/empresas/sessao_recanto_01"))
                .andExpect(status().isNoContent());

        assertEquals(false, empresa.getAtivo());
        assertTrue(empresa.getDataExclusao() != null);
        verify(empresaRepository).save(empresa);
        verify(servicoWppConnect).desconectar("sessao_recanto_01");
    }

    @Test
    @DisplayName("DELETE /empresas/{sessao}: rejeita requisição sem credencial de admin")
    void deveRecusarExclusaoSemAdmin() throws Exception {
        mockMvc.perform(delete("/empresas/sessao_recanto_01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /empresas/{sessao}: retorna 404 para empresa inexistente ou já inativa")
    void deveRetornar404ParaEmpresaInexistenteOuJaInativa() throws Exception {
        when(empresaRepository.buscarPorSessaoComModulos("sessao_inexistente")).thenReturn(null);
        when(empresaRepository.findBySessaoWhatsapp("sessao_inexistente")).thenReturn(null);
        autenticarComoAdmin();

        mockMvc.perform(delete("/empresas/sessao_inexistente"))
                .andExpect(status().isNotFound());

        Empresa inativa = empresaDe(2L, "sessao_inativa");
        inativa.setAtivo(false);
        when(empresaRepository.buscarPorSessaoComModulos("sessao_inativa")).thenReturn(inativa);
        when(empresaRepository.findBySessaoWhatsapp("sessao_inativa")).thenReturn(inativa);

        mockMvc.perform(delete("/empresas/sessao_inativa"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente: rejeita ativar previsão quando admin não liberou")
    void deveRejeitarAtivacaoPrevisaoSemPermissaoDoAdmin() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermitePrevisaoTempo(false);
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(empresa);
        autenticarComoClienteDe(empresa);

        String json = """
                {
                    "previsaoTempoAtiva": true
                }
                """;

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("A previsão do tempo precisa ser liberada pelo administrador."));
    }

    @Test
    @DisplayName("PUT /empresas/{sessao}/cliente: aceita ativar previsão quando admin liberou")
    void deveAceitarAtivacaoPrevisaoQuandoAdminLiberou() throws Exception {
        Empresa empresa = empresaDe(1L, "sessao_recanto_01");
        empresa.setPermitePrevisaoTempo(true);
        when(empresaRepository.findBySessaoWhatsapp("sessao_recanto_01")).thenReturn(empresa);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
        autenticarComoClienteDe(empresa);

        String json = """
                {
                    "previsaoTempoAtiva": true
                }
                """;

        mockMvc.perform(put("/empresas/sessao_recanto_01/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previsaoTempoAtiva").value(true))
                .andExpect(jsonPath("$.permitePrevisaoTempo").value(true))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("DELETE /empresas/{sessao} recusa com 403 para ADMIN_OPERACIONAL")
    void deveRecusarExclusaoParaAdminOperacional() throws Exception {
        autenticarComoRole(Role.ADMIN_OPERACIONAL);
        mockMvc.perform(delete("/empresas/sessao_recanto"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /empresas recusa com 403 para ADMIN_LEITURA")
    void deveRecusarCriacaoParaAdminLeitura() throws Exception {
        autenticarComoRole(Role.ADMIN_LEITURA);
        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPost("")))
                .andExpect(status().isForbidden());
    }
}
