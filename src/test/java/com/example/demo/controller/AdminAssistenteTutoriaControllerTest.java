package com.example.demo.controller;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.dto.assistente.DiagnosticoResponseDTO;
import com.example.demo.dto.assistente.DiagnosticoStatusDTO;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import com.example.demo.servico.assistente.DiagnosticoModulosService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAssistenteTutoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAssistenteTutoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DiagnosticoModulosService diagnosticoModulosService;

    @MockitoBean
    private AssistenteTutoriaService assistenteTutoriaService;

    @Test
    @DisplayName("GET /admin/assistente-tutoria/diagnostico com token valido responde 200")
    void deveExecutarDiagnosticoComSucesso() throws Exception {
        DiagnosticoResponseDTO mockResp = new DiagnosticoResponseDTO(
                "OPERACIONAL",
                Instant.now(),
                List.of(new DiagnosticoStatusDTO("Auth", "OPERACIONAL", "Ativo", 15L))
        );
        when(diagnosticoModulosService.executarDiagnostico()).thenReturn(mockResp);

        mockMvc.perform(get("/admin/assistente-tutoria/diagnostico")
                        .header("x-admin-token", "sua-chave-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusGeral").value("OPERACIONAL"))
                .andExpect(jsonPath("$.modulos[0].modulo").value("Auth"));
    }

    @Test
    @DisplayName("GET /admin/assistente-tutoria/diagnostico sem token responde 401")
    void deveRejeitarDiagnosticoSemToken() throws Exception {
        mockMvc.perform(get("/admin/assistente-tutoria/diagnostico"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/assistente-tutoria/chat com token valido responde 200")
    void deveResponderChatComSucesso() throws Exception {
        AssistenteChatResponseDTO mockResp = new AssistenteChatResponseDTO(
                "Passo a passo de configuracao.",
                "DIAGNOSTICO",
                Instant.now(),
                List.of("Sugestao 1")
        );
        when(assistenteTutoriaService.responder(any())).thenReturn(mockResp);

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Como configuro o WhatsApp?", "DIAGNOSTICO");

        mockMvc.perform(post("/admin/assistente-tutoria/chat")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Passo a passo de configuracao."))
                .andExpect(jsonPath("$.modo").value("DIAGNOSTICO"));
    }

    @Test
    @DisplayName("POST /admin/assistente-tutoria/chat sem token responde 401")
    void deveRejeitarChatSemToken() throws Exception {
        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Duvida", "DIAGNOSTICO");

        mockMvc.perform(post("/admin/assistente-tutoria/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/assistente-tutoria/chat com mensagem em branco responde 400")
    void deveRejeitarChatMensagemEmBranco() throws Exception {
        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("", "DIAGNOSTICO");

        mockMvc.perform(post("/admin/assistente-tutoria/chat")
                        .header("x-admin-token", "sua-chave-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
