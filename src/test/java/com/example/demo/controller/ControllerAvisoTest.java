package com.example.demo.controller;

import com.example.demo.model.Aviso;
import com.example.demo.model.TipoAviso;
import com.example.demo.repository.AvisoRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerAviso.class)
@AutoConfigureMockMvc(addFilters = false)
class ControllerAvisoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvisoRepository avisoRepository;

    @Test
    @DisplayName("GET /avisos devolve só os avisos ativos, sem exigir token")
    void deveListarSomenteAtivos() throws Exception {
        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of(aviso(1L, true)));

        mockMvc.perform(get("/avisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tipo").value("TUTORIAL"))
                .andExpect(jsonPath("$[0].titulo").value("Como conectar o WhatsApp"))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    @DisplayName("GET /admin/avisos sem token retorna 401")
    void deveRecusarListagemAdminSemToken() throws Exception {
        mockMvc.perform(get("/admin/avisos")).andExpect(status().isUnauthorized());
        verify(avisoRepository, never()).findAllByOrderByIdDesc();
    }

    @Test
    @DisplayName("GET /admin/avisos devolve ativos e inativos")
    void deveListarTodosNoAdmin() throws Exception {
        when(avisoRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
                aviso(2L, false),
                aviso(1L, true)
        ));

        mockMvc.perform(get("/admin/avisos").header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].ativo").value(false))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    @DisplayName("POST /admin/avisos grava o aviso e devolve 201")
    void deveCriarAviso() throws Exception {
        when(avisoRepository.save(any(Aviso.class))).thenAnswer(invocacao -> {
            Aviso aviso = invocacao.getArgument(0);
            aviso.setId(10L);
            return aviso;
        });

        mockMvc.perform(post("/admin/avisos")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "tipo": "MANUTENCAO",
                              "titulo": "Janela de manutenção",
                              "conteudo": "WPPConnect fica offline das 2h às 4h.",
                              "link": "https://status.example.com",
                              "ativo": true
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.tipo").value("MANUTENCAO"));

        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository).save(captor.capture());
        Aviso salvo = captor.getValue();
        assertEquals(TipoAviso.MANUTENCAO, salvo.getTipo());
        assertEquals("Janela de manutenção", salvo.getTitulo());
        assertTrue(Boolean.TRUE.equals(salvo.getAtivo()));
    }

    @Test
    @DisplayName("PUT /admin/avisos/{id} atualiza um aviso existente")
    void deveAtualizarAviso() throws Exception {
        Aviso existente = aviso(1L, true);
        when(avisoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(avisoRepository.save(any(Aviso.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(put("/admin/avisos/1")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "tipo": "PRECO",
                              "titulo": "Planos atualizados",
                              "conteudo": "IA Gemini no plano Pro.",
                              "ativo": false
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PRECO"))
                .andExpect(jsonPath("$.ativo").value(false));

        assertEquals(TipoAviso.PRECO, existente.getTipo());
        assertEquals("Planos atualizados", existente.getTitulo());
    }

    @Test
    @DisplayName("DELETE /admin/avisos/{id} remove o aviso")
    void deveExcluirAviso() throws Exception {
        when(avisoRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/admin/avisos/1").header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isNoContent());

        verify(avisoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("DELETE /admin/avisos/{id} inexistente retorna 404")
    void deveRetornar404AoExcluirInexistente() throws Exception {
        when(avisoRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/admin/avisos/99").header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isNotFound());
    }

    private static Aviso aviso(Long id, boolean ativo) {
        Aviso aviso = new Aviso();
        aviso.setId(id);
        aviso.setTipo(TipoAviso.TUTORIAL);
        aviso.setTitulo("Como conectar o WhatsApp");
        aviso.setConteudo("Escaneie o QR Code.");
        aviso.setLink("https://www.youtube.com/watch?v=exemplo");
        aviso.setAtivo(ativo);
        return aviso;
    }
}
