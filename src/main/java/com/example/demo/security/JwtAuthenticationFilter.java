package com.example.demo.security;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final String adminApiKey;

    public JwtAuthenticationFilter(
            TokenService tokenService,
            UsuarioRepository usuarioRepository,
            @Value("${admin.api.key}") String adminApiKey) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.adminApiKey = adminApiKey;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticarPorJwt(request);
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticarPorHeadersLegados(request);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarPorJwt(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }

        String token = header.substring(7).trim();
        if (token.isBlank()) {
            return;
        }

        try {
            String email = tokenService.validarToken(token);
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
            if (usuario == null) {
                return;
            }
            var autenticacao = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (Exception ignorado) {
            SecurityContextHolder.clearContext();
        }
    }

    private void autenticarPorHeadersLegados(HttpServletRequest request) {
        String adminToken = request.getHeader("x-admin-token");
        if (adminToken != null && adminToken.equals(adminApiKey)) {
            var autenticacao = new UsernamePasswordAuthenticationToken(
                    "admin-api-key",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
            return;
        }

        String clienteToken = request.getHeader("x-cliente-token");
        if (clienteToken != null && !clienteToken.isBlank()) {
            var autenticacao = new UsernamePasswordAuthenticationToken(
                    "cliente-token",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        }
    }
}
