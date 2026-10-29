package com.example.demo.security;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(tokenService, usuarioRepository, "sua-chave-admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("O cabeçalho x-cliente-token não autentica a requisição")
    void xClienteTokenNaoAutentica() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-cliente-token", "qualquer-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("O cabeçalho x-admin-token válido autentica como ROLE_ADMIN")
    void xAdminTokenValidoAutentica() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-admin-token", "sua-chave-admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin-api-key");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("O cabeçalho x-admin-token inválido não autentica")
    void xAdminTokenInvalidoNaoAutentica() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-admin-token", "chave-errada");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT válido autentica com o usuário e autoridade ROLE_CLIENTE")
    void jwtValidoAutenticaUsuario() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-jwt-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@empresa.com");
        usuario.setRole(Role.CLIENTE);
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("sessao_01");
        usuario.setEmpresa(empresa);

        when(tokenService.validarToken("token-jwt-valido")).thenReturn("cliente@empresa.com");
        when(usuarioRepository.findByEmailIgnoreCase("cliente@empresa.com")).thenReturn(Optional.of(usuario));

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(usuario);
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENTE");
        verify(filterChain).doFilter(request, response);
    }
}
