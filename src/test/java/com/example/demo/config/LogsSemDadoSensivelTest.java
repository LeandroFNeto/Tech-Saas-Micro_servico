package com.example.demo.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.LimiteTentativasLogin;
import com.example.demo.servico.LoginBloqueadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogsSemDadoSensivelTest {

    private static final String SENHA_MASTER = "SenhaMasterDeTeste123";
    private static final String SENHA_LEGADA = "senha-antiga-de-teste";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final ListAppender<ILoggingEvent> captura = new ListAppender<>();
    private final Logger raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @BeforeEach
    void iniciarCaptura() {
        captura.start();
        raiz.addAppender(captura);
        lenient().when(usuarioRepository.existsByEmailIgnoreCase(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        lenient().when(empresaRepository.findAll()).thenReturn(List.of());
        lenient().when(usuarioRepository.findByRole(Role.CLIENTE)).thenReturn(List.of());
    }

    @AfterEach
    void encerrarCaptura() {
        raiz.detachAppender(captura);
        captura.stop();
    }

    private String log() {
        return captura.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }

    @Test
    @DisplayName("Login bloqueado registra o e-mail mascarado e nunca a senha")
    void loginBloqueadoComEmailMascarado() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin();
        for (int i = 0; i < 5; i++) {
            limite.registrarFalha("cliente.teste@empresa.com", "9.9.9.9");
        }
        assertThrows(LoginBloqueadoException.class, () -> limite.exigirLivre("cliente.teste@empresa.com", "9.9.9.9"));

        String saida = log();
        assertTrue(saida.contains("Login bloqueado"));
        assertFalse(saida.contains("cliente.teste@empresa.com"));
        assertFalse(saida.toLowerCase().contains("senha="));
    }

    @Test
    @DisplayName("Seed de cliente não escreve senha no log nem o e-mail completo")
    void seedDeClienteSemSenhaNoLog() {
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("sessao_a");
        empresa.setId(4242L);
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        new AdminInitializer(usuarioRepository, empresaRepository, encoder, "admin@gamb.com", SENHA_MASTER, "").run();

        String saida = log();
        assertTrue(saida.contains("senha aleatória"));
        assertTrue(saida.contains("4242"));
        assertFalse(saida.contains("sessao_a"));
        assertFalse(saida.contains(SENHA_MASTER));
        assertFalse(saida.contains("admin@gamb.com"));
        assertFalse(saida.contains("sessao_a@gamb.com.br"));
    }

    @Test
    @DisplayName("Bloqueio por senha legada registra só a quantidade e e-mails mascarados")
    void bloqueioLegadoSoComQuantidadeEEmailsMascarados() {
        Usuario legada = new Usuario();
        legada.setEmail("sessao_a@gamb.com.br");
        legada.setRole(Role.CLIENTE);
        legada.setSenha(encoder.encode(SENHA_LEGADA));
        when(usuarioRepository.findByRole(Role.CLIENTE)).thenReturn(List.of(legada));

        new AdminInitializer(usuarioRepository, empresaRepository, encoder, "admin@gamb.com", SENHA_MASTER, SENHA_LEGADA).run();

        String saida = log();
        assertTrue(saida.contains("bloqueadas por senha legada: 1"));
        assertFalse(saida.contains("sessao_a@gamb.com.br"));
        assertFalse(saida.contains(SENHA_LEGADA));
    }

    @Test
    @DisplayName("Senha do admin vazia avisa no log")
    void senhaVaziaAvisaNoLog() {
        new AdminInitializer(usuarioRepository, empresaRepository, encoder, "admin@gamb.com", "", "").run();

        assertTrue(captura.list.stream().anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("ADMIN_DEFAULT_PASSWORD vazia")));
    }

    @Test
    @DisplayName("Curinga no CORS é ignorado com aviso no log")
    void curingaDoCorsAvisaNoLog() {
        assertEquals(0, new CorsConfig("*").origensPermitidas().length);

        assertTrue(captura.list.stream().anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("curinga ignorado")));
    }
}
