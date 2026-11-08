package com.example.demo.controller;

import com.example.demo.dto.auth.AlterarSenhaDTO;
import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.LoginResponseDTO;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.servico.IpDoCliente;
import com.example.demo.servico.LimiteTentativasLogin;
import com.example.demo.servico.ServicoAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerAuth.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({IpDoCliente.class, LimiteTentativasLogin.class})
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

    private static final String CORPO_LOGIN = "{\"email\":\"limite@empresa.com\",\"senha\":\"SenhaForte123\"}";

    @Test
    @DisplayName("Sexta tentativa depois de 5 falhas responde 429 com Retry-After, mesmo com senha certa")
    void login_aposCincoFalhas_retorna429ComRetryAfter() throws Exception {
        when(servicoAuth.login(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_LOGIN))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Senha incorreta"));
        }

        when(servicoAuth.login(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO("token.jwt.valido", "limite@empresa.com", "CLIENTE", "Empresa", "sessao"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(CORPO_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Login certo zera a contagem de falhas do e-mail e IP")
    void login_certoZeraContagem() throws Exception {
        String corpo = "{\"email\":\"zera@empresa.com\",\"senha\":\"SenhaForte123\"}";
        when(servicoAuth.login(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta"));
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnauthorized());
        }

        when(servicoAuth.login(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO("token.jwt.valido", "zera@empresa.com", "CLIENTE", "Empresa", "sessao"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());

        when(servicoAuth.login(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Nova senha com menos de 10 caracteres retorna 400")
    void alterarSenha_senhaCurta_retorna400() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@empresa.com");
        var auth = new UsernamePasswordAuthenticationToken(usuario, null, java.util.List.of());

        mockMvc.perform(put("/auth/alterar-senha")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"SenhaAntiga123\",\"novaSenha\":\"curta1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
