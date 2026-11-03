package com.example.demo.controller;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteAssistenteChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClienteAssistenteChatTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssistenteTutoriaService assistenteTutoriaService;

    private Empresa empresa;
    private Usuario usuarioCliente;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(10L);
        empresa.setNome("Recanto dos Testes");
        empresa.setSessaoWhatsapp("sessao-automacao");

        usuarioCliente = new Usuario();
        usuarioCliente.setId(20L);
        usuarioCliente.setEmail("cliente@recanto.com.br");
        usuarioCliente.setRole(Role.CLIENTE);
        usuarioCliente.setEmpresa(empresa);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoCliente() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        usuarioCliente,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                )
        );
    }

    @Test
    @DisplayName("POST /api/cliente/assistente/chat com usuario autenticado responde 200 no modo SUPORTE")
    void deveResponderChatSuporteParaClienteAutenticado() throws Exception {
        autenticarComoCliente();

        AssistenteChatResponseDTO mockResp = new AssistenteChatResponseDTO(
                "Para configurar o WhatsApp, acesse o perfil.",
                "SUPORTE",
                Instant.now(),
                List.of("Sugestao 1")
        );
        when(assistenteTutoriaService.responderCliente(eq(empresa), any())).thenReturn(mockResp);

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Como configuro o WhatsApp?", "SUPORTE");

        mockMvc.perform(post("/api/cliente/assistente/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Para configurar o WhatsApp, acesse o perfil."))
                .andExpect(jsonPath("$.modo").value("SUPORTE"));
    }

    @Test
    @DisplayName("POST /api/cliente/assistente/chat com usuario autenticado responde 200 no modo TESTE_VIRTUAL")
    void deveResponderChatTesteVirtualParaClienteAutenticado() throws Exception {
        autenticarComoCliente();

        AssistenteChatResponseDTO mockResp = new AssistenteChatResponseDTO(
                "Olá! Sou o bot da empresa Recanto dos Testes.",
                "TESTE_VIRTUAL",
                Instant.now(),
                List.of("Menu")
        );
        when(assistenteTutoriaService.responderCliente(eq(empresa), any())).thenReturn(mockResp);

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Oi", "TESTE_VIRTUAL");

        mockMvc.perform(post("/api/cliente/assistente/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Olá! Sou o bot da empresa Recanto dos Testes."))
                .andExpect(jsonPath("$.modo").value("TESTE_VIRTUAL"));
    }

    @Test
    @DisplayName("POST /api/cliente/assistente/chat sem autenticacao responde 401")
    void deveRejeitarSemAutenticacao() throws Exception {
        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Duvida", "SUPORTE");

        mockMvc.perform(post("/api/cliente/assistente/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/cliente/assistente/chat com mensagem em branco responde 400")
    void deveRejeitarMensagemEmBranco() throws Exception {
        autenticarComoCliente();

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("   ", "SUPORTE");

        mockMvc.perform(post("/api/cliente/assistente/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
