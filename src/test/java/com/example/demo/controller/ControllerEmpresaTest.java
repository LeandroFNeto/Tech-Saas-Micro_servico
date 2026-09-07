package com.example.demo.controller;

import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
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
class ControllerEmpresaTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EmpresaRepository empresaRepository;

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
}
