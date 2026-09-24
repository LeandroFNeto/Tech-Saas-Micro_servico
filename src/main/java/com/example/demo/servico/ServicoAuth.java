package com.example.demo.servico;

import com.example.demo.dto.auth.AlterarSenhaDTO;
import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.LoginResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoAuth {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public ServicoAuth(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public static String emailAcessoDaSessao(String sessaoWhatsapp) {
        if (sessaoWhatsapp == null || sessaoWhatsapp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sessão WhatsApp inválida");
        }
        return sessaoWhatsapp.toLowerCase().replaceAll("\\s+", "") + "@gamb.com.br";
    }

    public void garantirEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já cadastrado");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(dto.email().trim())
                .filter(encontrado -> passwordEncoder.matches(dto.senha(), encontrado.getSenha()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta"));

        String nome = usuario.getEmpresa() != null ? usuario.getEmpresa().getNome() : "Administrador";
        String sessao = usuario.getEmpresa() != null ? usuario.getEmpresa().getSessaoWhatsapp() : null;
        return new LoginResponseDTO(
                tokenService.gerarToken(usuario),
                usuario.getEmail(),
                usuario.getRole().name(),
                nome,
                sessao
        );
    }

    @Transactional
    public void alterarSenha(String email, AlterarSenhaDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autorizado"));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void resetarSenhaDoCliente(String sessaoWhatsapp, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmpresa_SessaoWhatsapp(sessaoWhatsapp)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário do cliente não encontrado"));

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void criarUsuarioCliente(Empresa empresa, String email, String senha) {
        String emailAcesso = emailAcessoDaSessao(empresa.getSessaoWhatsapp());
        if (usuarioRepository.existsByEmailIgnoreCase(emailAcesso)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(emailAcesso);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setRole(Role.CLIENTE);
        usuario.setEmpresa(empresa);
        usuarioRepository.save(usuario);
    }
}
