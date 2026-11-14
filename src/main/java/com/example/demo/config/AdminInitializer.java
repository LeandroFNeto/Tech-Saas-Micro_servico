package com.example.demo.config;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.ServicoAuth;
import com.example.demo.util.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    static final int TAMANHO_MINIMO_SENHA_MASTER = 12;
    private static final int TAMANHO_SENHA_ALEATORIA = 32;
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String emailMaster;
    private final String senhaMaster;
    private final String senhaLegadaABloquear;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.default.email:usuario@gamb.com}") String emailMaster,
            @Value("${admin.default.password:}") String senhaMaster,
            @Value("${admin.senha-legada-a-bloquear:}") String senhaLegadaABloquear) {
        if (senhaMaster != null && !senhaMaster.isBlank() && senhaMaster.length() < TAMANHO_MINIMO_SENHA_MASTER) {
            throw new IllegalStateException("ADMIN_DEFAULT_PASSWORD precisa ter ao menos 12 caracteres");
        }
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailMaster = emailMaster;
        this.senhaMaster = senhaMaster == null ? "" : senhaMaster;
        this.senhaLegadaABloquear = senhaLegadaABloquear == null ? "" : senhaLegadaABloquear;
    }

    @Override
    public void run(String... args) {
        seedMaster();
        seedClientesJaCadastrados();
        bloquearContasComSenhaLegada();
    }

    private void seedMaster() {
        if (usuarioRepository.existsByEmailIgnoreCase(emailMaster)) {
            log.info("Usuário master {} já existe — seed ignorado.", MascaraLog.email(emailMaster));
            return;
        }

        if (senhaMaster.isBlank()) {
            log.warn("ADMIN_DEFAULT_PASSWORD vazia: usuário master {} não foi criado.", MascaraLog.email(emailMaster));
            return;
        }

        Usuario master = new Usuario();
        master.setEmail(emailMaster);
        master.setSenha(passwordEncoder.encode(senhaMaster));
        master.setRole(Role.ADMIN);
        usuarioRepository.save(master);
        log.info("Usuário master {} criado com role ADMIN.", MascaraLog.email(emailMaster));
    }

    private void seedClientesJaCadastrados() {
        for (Empresa empresa : empresaRepository.findAll()) {
            if (empresa.getSessaoWhatsapp() == null || empresa.getSessaoWhatsapp().isBlank()) {
                continue;
            }

            String email = ServicoAuth.emailAcessoDaSessao(empresa.getSessaoWhatsapp());
            if (usuarioRepository.existsByEmailIgnoreCase(email)) {
                continue;
            }

            Usuario cliente = new Usuario();
            cliente.setEmail(email);
            cliente.setSenha(passwordEncoder.encode(senhaAleatoria()));
            cliente.setRole(Role.CLIENTE);
            cliente.setEmpresa(empresa);
            usuarioRepository.save(cliente);
            log.info("Usuário CLIENTE {} criado para a empresa {} com senha aleatória; o admin deve redefinir a senha.", MascaraLog.email(email), empresa.getId());
        }
    }

    private void bloquearContasComSenhaLegada() {
        if (senhaLegadaABloquear.isBlank()) {
            return;
        }
        List<String> bloqueados = new ArrayList<>();
        for (Usuario cliente : usuarioRepository.findByRole(Role.CLIENTE)) {
            if (cliente.getSenha() != null && passwordEncoder.matches(senhaLegadaABloquear, cliente.getSenha())) {
                cliente.setSenha(passwordEncoder.encode(senhaAleatoria()));
                usuarioRepository.save(cliente);
                bloqueados.add(MascaraLog.email(cliente.getEmail()));
            }
        }
        log.warn("Contas CLIENTE bloqueadas por senha legada: {} {}", bloqueados.size(), bloqueados);
    }

    private static String senhaAleatoria() {
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA_ALEATORIA);
        for (int i = 0; i < TAMANHO_SENHA_ALEATORIA; i++) {
            senha.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
