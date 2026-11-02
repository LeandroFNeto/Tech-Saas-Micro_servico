package com.example.demo.controller;

import com.example.demo.dto.auth.AlterarSenhaDTO;
import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.LoginResponseDTO;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.servico.ServicoAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerAuth.class)
@AutoConfigureMockMvc(addFilters = false)
class ControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoAuth servicoAuth;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Login com dados validos retorna 200 e LoginResponseDTO")
    void login_dadosValidos_retorna200() throws Exception {
        LoginResponseDTO responseDTO = new LoginResponseDTO(
                "token.jwt.valido",
                "teste@empresa.com",
                "CLIENTE",
                "Empresa Teste",
                "sessao-teste"
        );
        when(servicoAuth.login(any(LoginRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teste@empresa.com\",\"senha\":\"SenhaForte123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt.valido"))
                .andExpect(jsonPath("$.email").value("teste@empresa.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    @DisplayName("Login com email em formato invalido retorna 400 Bad Request")
    void login_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email-invalido\",\"senha\":\"SenhaForte123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Alterar senha sem autenticacao retorna 401 Unauthorized")
    void alterarSenha_semAutenticacao_retorna401() throws Exception {
        mockMvc.perform(put("/auth/alterar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"SenhaAntiga123\",\"novaSenha\":\"NovaSenhaForte123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Alterar senha com autenticacao valida retorna 204 No Content")
    void alterarSenha_autenticado_retorna204() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@empresa.com");
        var auth = new UsernamePasswordAuthenticationToken(usuario, null, java.util.List.of());

        mockMvc.perform(put("/auth/alterar-senha")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"SenhaAntiga123\",\"novaSenha\":\"NovaSenhaForte123\"}"))
                .andExpect(status().isNoContent());

        verify(servicoAuth).alterarSenha(eq("cliente@empresa.com"), any(AlterarSenhaDTO.class));
    }
}
