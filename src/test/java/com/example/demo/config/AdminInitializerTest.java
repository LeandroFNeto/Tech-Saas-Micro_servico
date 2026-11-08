package com.example.demo.config;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    private static final String SENHA_ANTIGA = "senha-antiga-de-teste";
    private static final String SENHA_MASTER = "SenhaMasterDeTeste123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        lenient().when(empresaRepository.findAll()).thenReturn(List.of());
        lenient().when(usuarioRepository.findByRole(Role.CLIENTE)).thenReturn(List.of());
    }

    private AdminInitializer inicializador(String senhaMaster, String senhaLegada) {
        return new AdminInitializer(usuarioRepository, empresaRepository, encoder, "admin@gamb.com", senhaMaster, senhaLegada);
    }

    private Empresa empresa(String sessao) {
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp(sessao);
        return empresa;
    }

    @Test
    @DisplayName("Seed de cliente cria senha aleatória que não confere com nenhum valor conhecido")
    void seedClienteComSenhaAleatoria() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresa("sessao_a"), empresa("sessao_b")));

        inicializador(SENHA_MASTER, "").run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(3)).save(captor.capture());
        List<Usuario> clientes = captor.getAllValues().stream().filter(u -> u.getRole() == Role.CLIENTE).toList();
        assertEquals(2, clientes.size());
        assertFalse(encoder.matches(SENHA_ANTIGA, clientes.get(0).getSenha()));
        assertFalse(encoder.matches(SENHA_ANTIGA, clientes.get(1).getSenha()));
        assertNotEquals(clientes.get(0).getSenha(), clientes.get(1).getSenha());
    }

    @Test
    @DisplayName("Admin é criado com a senha configurada")
    void criaAdminComSenhaConfigurada() {
        inicializador(SENHA_MASTER, "").run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
        assertTrue(encoder.matches(SENHA_MASTER, captor.getValue().getSenha()));
    }

    @Test
    @DisplayName("Senha do admin vazia não cria o admin")
    void senhaVaziaNaoCriaAdmin() {
        inicializador("", "").run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Senha do admin com menos de 12 caracteres não sobe")
    void senhaCurtaNaoSobe() {
        assertThrows(IllegalStateException.class, () -> inicializador("curta12345", ""));
    }

    @Test
    @DisplayName("Bloqueia contas de cliente que conferem com a senha legada e preserva as demais")
    void bloqueiaContasComSenhaLegada() {
        Usuario legada = new Usuario();
        legada.setEmail("sessao_a@gamb.com.br");
        legada.setRole(Role.CLIENTE);
        legada.setSenha(encoder.encode(SENHA_ANTIGA));

        Usuario trocada = new Usuario();
        trocada.setEmail("sessao_b@gamb.com.br");
        trocada.setRole(Role.CLIENTE);
        String hashTrocado = encoder.encode("OutraSenhaDeTeste123");
        trocada.setSenha(hashTrocado);

        when(usuarioRepository.findByRole(Role.CLIENTE)).thenReturn(List.of(legada, trocada));

        inicializador(SENHA_MASTER, SENHA_ANTIGA).run();

        assertFalse(encoder.matches(SENHA_ANTIGA, legada.getSenha()));
        assertEquals(hashTrocado, trocada.getSenha());
    }

    @Test
    @DisplayName("Sem a variável da senha legada nenhuma conta é consultada")
    void semVariavelNaoBloqueia() {
        inicializador(SENHA_MASTER, "").run();

        verify(usuarioRepository, never()).findByRole(any());
    }
}
