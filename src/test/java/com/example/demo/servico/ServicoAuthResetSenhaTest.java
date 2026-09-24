package com.example.demo.servico;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicoAuthResetSenhaTest {

    @Test
    void deveGravarHashSemConferirSenhaAtual() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setSenha("hash-antigo");
        when(usuarios.findByEmpresa_SessaoWhatsapp("sessao_recanto_01")).thenReturn(Optional.of(usuario));
        when(encoder.encode("NovaSenha456")).thenReturn("$2a$hash-novo");

        new ServicoAuth(usuarios, encoder, null).resetarSenhaDoCliente("sessao_recanto_01", "NovaSenha456");

        assertEquals("$2a$hash-novo", usuario.getSenha());
        verify(encoder).encode("NovaSenha456");
        verify(usuarios).save(usuario);
    }
}