package com.example.demo.controller;

import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.ServicoAuth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("PUT /empresas/{sessao}/cliente persiste urlsGaleria e foto principal")
    void deveAtualizarGaleriaPeloCliente() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
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
                              ]
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkFotoPrincipal")
                        .value("https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg"))
                .andExpect(jsonPath("$.urlsGaleria[0]")
                        .value("https://res.cloudinary.com/demo/image/upload/v1/foto1.jpg"))
                .andExpect(jsonPath("$.urlsGaleria.length()").value(2));

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v1/hero_image.jpg",
                empresa.getLinkFotoPrincipal());
        assertEquals(2, empresa.getUrlsGaleria().size());
    }
}
