package com.example.demo.config;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.ServicoAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    static final String SENHA_CLIENTES_LEGADOS = "mudar123";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String emailMaster;
    private final String senhaMaster;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.default.email:usuario@gamb.com}") String emailMaster,
            @Value("${admin.default.password:SenhaMaster123}") String senhaMaster) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailMaster = emailMaster;
        this.senhaMaster = senhaMaster;
    }

    @Override
    public void run(String... args) {
        seedMaster();
        seedClientesJaCadastrados();
    }

    private void seedMaster() {
        if (usuarioRepository.existsByEmailIgnoreCase(emailMaster)) {
            log.info("Usuário master {} já existe — seed ignorado.", emailMaster);
            return;
        }

        Usuario master = new Usuario();
        master.setEmail(emailMaster);
        master.setSenha(passwordEncoder.encode(senhaMaster));
        master.setRole(Role.ADMIN);
        usuarioRepository.save(master);
        log.info("Usuário master {} criado com role ADMIN.", emailMaster);
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
            cliente.setSenha(passwordEncoder.encode(SENHA_CLIENTES_LEGADOS));
            cliente.setRole(Role.CLIENTE);
            cliente.setEmpresa(empresa);
            usuarioRepository.save(cliente);
            log.info("Usuário CLIENTE {} criado para a empresa já cadastrada {}.", email, empresa.getSessaoWhatsapp());
        }
    }
}
