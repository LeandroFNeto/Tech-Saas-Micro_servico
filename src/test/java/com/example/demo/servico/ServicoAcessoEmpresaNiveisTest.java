package com.example.demo.servico;

import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ServicoAcessoEmpresaNiveisTest {

    @Mock
    private EmpresaRepository empresaRepository;

    private ServicoAcessoEmpresa servico;
    private final String apiKey = "chave-secreta-admin";

    @BeforeEach
    void setUp() {
        servico = new ServicoAcessoEmpresa(empresaRepository, apiKey);
    }

    private Authentication autenticarUsuario(Role role) {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@exemplo.com");
        usuario.setRole(role);
        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
    }

    @Test
    @DisplayName("Role helpers devem classificar permissões corretamente")
    void roleHelpersCorretos() {
        assertTrue(Role.ADMIN.isAdmin());
        assertTrue(Role.ADMIN.isSuperAdmin());
        assertTrue(Role.ADMIN.isOperacional());
        assertFalse(Role.ADMIN.isLeitura());

        assertTrue(Role.SUPER_ADMIN.isAdmin());
        assertTrue(Role.SUPER_ADMIN.isSuperAdmin());
        assertTrue(Role.SUPER_ADMIN.isOperacional());
        assertFalse(Role.SUPER_ADMIN.isLeitura());

        assertTrue(Role.ADMIN_OPERACIONAL.isAdmin());
        assertFalse(Role.ADMIN_OPERACIONAL.isSuperAdmin());
        assertTrue(Role.ADMIN_OPERACIONAL.isOperacional());
        assertFalse(Role.ADMIN_OPERACIONAL.isLeitura());

        assertTrue(Role.ADMIN_LEITURA.isAdmin());
        assertFalse(Role.ADMIN_LEITURA.isSuperAdmin());
        assertFalse(Role.ADMIN_LEITURA.isOperacional());
        assertTrue(Role.ADMIN_LEITURA.isLeitura());

        assertFalse(Role.CLIENTE.isAdmin());
        assertFalse(Role.CLIENTE.isSuperAdmin());
        assertFalse(Role.CLIENTE.isOperacional());
        assertFalse(Role.CLIENTE.isLeitura());
    }

    @Test
    @DisplayName("isSuperAdmin aceita API key e perfis SUPER_ADMIN e ADMIN")
    void isSuperAdminAceitaChaveEPerfisMaster() {
        assertTrue(servico.isSuperAdmin(null, apiKey));
        assertTrue(servico.isSuperAdmin(autenticarUsuario(Role.SUPER_ADMIN), null));
        assertTrue(servico.isSuperAdmin(autenticarUsuario(Role.ADMIN), null));
        assertFalse(servico.isSuperAdmin(autenticarUsuario(Role.ADMIN_OPERACIONAL), null));
        assertFalse(servico.isSuperAdmin(autenticarUsuario(Role.ADMIN_LEITURA), null));
        assertFalse(servico.isSuperAdmin(autenticarUsuario(Role.CLIENTE), null));
    }

    @Test
    @DisplayName("isLeitura identifica perfil ADMIN_LEITURA")
    void isLeituraIdentificaPerfilLeitura() {
        assertTrue(servico.isLeitura(autenticarUsuario(Role.ADMIN_LEITURA)));
        assertFalse(servico.isLeitura(autenticarUsuario(Role.SUPER_ADMIN)));
        assertFalse(servico.isLeitura(autenticarUsuario(Role.ADMIN_OPERACIONAL)));
    }

    @Test
    @DisplayName("exigirNaoLeitura bloqueia ADMIN_LEITURA com 403")
    void exigirNaoLeituraBloqueiaLeitura() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                servico.exigirNaoLeitura(autenticarUsuario(Role.ADMIN_LEITURA)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        servico.exigirNaoLeitura(autenticarUsuario(Role.ADMIN_OPERACIONAL));
        servico.exigirNaoLeitura(autenticarUsuario(Role.SUPER_ADMIN));
    }

    @Test
    @DisplayName("exigirSuperAdmin bloqueia não-super admin com 403")
    void exigirSuperAdminBloqueiaOutrosNiveis() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                servico.exigirSuperAdmin(autenticarUsuario(Role.ADMIN_OPERACIONAL), null));
        assertEquals(HttpStatus.FORBIDDEN, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                servico.exigirSuperAdmin(autenticarUsuario(Role.ADMIN_LEITURA), null));
        assertEquals(HttpStatus.FORBIDDEN, ex2.getStatusCode());

        servico.exigirSuperAdmin(autenticarUsuario(Role.SUPER_ADMIN), null);
        servico.exigirSuperAdmin(autenticarUsuario(Role.ADMIN), null);
        servico.exigirSuperAdmin(null, apiKey);
    }
}
