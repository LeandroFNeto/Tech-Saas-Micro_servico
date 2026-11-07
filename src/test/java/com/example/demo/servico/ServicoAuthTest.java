package com.example.demo.servico;

import com.example.demo.dto.auth.AlterarSenhaDTO;
import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.LoginResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoAuthTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @InjectMocks private ServicoAuth servicoAuth;

    @Test
    @DisplayName("emailAcessoDaSessao normaliza sessão e rejeita sessão nula ou em branco com 400")
    void deveNormalizarEmailAcessoDaSessao() {
        assertEquals("recanto_01@gamb.com.br", ServicoAuth.emailAcessoDaSessao(" Recanto _01 "));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ServicoAuth.emailAcessoDaSessao("   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("login autentica empresa ativa e bloqueia empresa inativa ou senha incorreta com 401")
    void deveAutenticarEmpresaAtivaEBloquearInativaOuSenhaErrada() {
        Empresa empresa = new Empresa();
        empresa.setNome("Recanto");
        empresa.setSessaoWhatsapp("recanto");
        empresa.setAtivo(true);

        Usuario usuario = new Usuario();
        usuario.setEmail("recanto@gamb.com.br");
        usuario.setSenha("hash");
        usuario.setRole(Role.CLIENTE);
        usuario.setEmpresa(empresa);

        when(usuarioRepository.findByEmailIgnoreCase("recanto@gamb.com.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt-gerado");

        LoginResponseDTO resp = servicoAuth.login(new LoginRequestDTO(" recanto@gamb.com.br ", "certa"));
        assertEquals("jwt-gerado", resp.token());
        assertEquals("recanto", resp.sessaoWhatsapp());

        ResponseStatusException erroSenha = assertThrows(ResponseStatusException.class,
                () -> servicoAuth.login(new LoginRequestDTO("recanto@gamb.com.br", "errada")));
        assertEquals(HttpStatus.UNAUTHORIZED, erroSenha.getStatusCode());

        empresa.setAtivo(false);
        ResponseStatusException erroInativa = assertThrows(ResponseStatusException.class,
                () -> servicoAuth.login(new LoginRequestDTO("recanto@gamb.com.br", "certa")));
        assertEquals(HttpStatus.UNAUTHORIZED, erroInativa.getStatusCode());
    }

    @Test
    @DisplayName("alterarSenha valida senha atual e grava novo hash")
    void deveAlterarSenhaValidandoSenhaAtual() {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@gamb.com.br");
        usuario.setSenha("hash-antigo");

        when(usuarioRepository.findByEmailIgnoreCase("cliente@gamb.com.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("atual-ok", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.matches("atual-errada", "hash-antigo")).thenReturn(false);
        when(passwordEncoder.encode("nova1234")).thenReturn("hash-novo");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAuth.alterarSenha("cliente@gamb.com.br", new AlterarSenhaDTO("atual-errada", "nova1234")));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());

        servicoAuth.alterarSenha("cliente@gamb.com.br", new AlterarSenhaDTO("atual-ok", "nova1234"));
        assertEquals("hash-novo", usuario.getSenha());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("resetarSenhaDoCliente retorna 404 quando sessão não possui usuário e atualiza quando existe")
    void deveResetarSenhaDoCliente() {
        when(usuarioRepository.findByEmpresa_SessaoWhatsapp("inexistente")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicoAuth.resetarSenhaDoCliente("inexistente", "nova1234"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        Usuario usuario = new Usuario();
        when(usuarioRepository.findByEmpresa_SessaoWhatsapp("recanto")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nova1234")).thenReturn("hash-reset");

        servicoAuth.resetarSenhaDoCliente("recanto", "nova1234");
        assertEquals("hash-reset", usuario.getSenha());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("criarUsuarioCliente e atualizarEmailDaSessao garantem unicidade de e-mail")
    void deveCriarUsuarioEAtualizarEmailDaSessao() {
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("recanto");

        when(usuarioRepository.existsByEmailIgnoreCase("recanto@gamb.com.br")).thenReturn(true);
        ResponseStatusException duplicado = assertThrows(ResponseStatusException.class,
                () -> servicoAuth.criarUsuarioCliente(empresa, "qualquer", "123456"));
        assertEquals(HttpStatus.BAD_REQUEST, duplicado.getStatusCode());

        when(usuarioRepository.existsByEmailIgnoreCase("recanto@gamb.com.br")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash-123");
        servicoAuth.criarUsuarioCliente(empresa, "qualquer", "123456");
        verify(usuarioRepository).save(any(Usuario.class));

        Usuario existente = new Usuario();
        existente.setEmail("recanto@gamb.com.br");
        when(usuarioRepository.findByEmpresa_SessaoWhatsapp("recanto")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailIgnoreCase("recanto_novo@gamb.com.br")).thenReturn(false);

        servicoAuth.atualizarEmailDaSessao("recanto", "recanto_novo");
        assertEquals("recanto_novo@gamb.com.br", existente.getEmail());

        when(usuarioRepository.existsByEmailIgnoreCase("ocupada@gamb.com.br")).thenReturn(true);
        ResponseStatusException conflito = assertThrows(ResponseStatusException.class,
                () -> servicoAuth.atualizarEmailDaSessao("recanto", "ocupada"));
        assertEquals(HttpStatus.BAD_REQUEST, conflito.getStatusCode());
        verify(usuarioRepository, never()).delete(any());
    }
}
