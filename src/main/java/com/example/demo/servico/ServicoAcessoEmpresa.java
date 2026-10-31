package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoAcessoEmpresa {

    private final EmpresaRepository empresaRepository;
    private final String adminApiKey;

    public ServicoAcessoEmpresa(
            EmpresaRepository empresaRepository,
            @Value("${admin.api.key}") String adminApiKey) {
        this.empresaRepository = empresaRepository;
        this.adminApiKey = adminApiKey;
    }

    public boolean isAdmin(Authentication autenticacao, String adminToken) {
        if (adminToken != null && adminToken.equals(adminApiKey)) {
            return true;
        }
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public boolean isSuperAdmin(Authentication autenticacao, String adminToken) {
        if (adminToken != null && adminToken.equals(adminApiKey)) {
            return true;
        }
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }
        if (autenticacao.getPrincipal() instanceof Usuario usuario) {
            return usuario.getRole() != null && usuario.getRole().isSuperAdmin();
        }
        boolean temAdmin = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        boolean temRestricao = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN_OPERACIONAL".equals(a) || "ROLE_ADMIN_LEITURA".equals(a));
        return temAdmin && !temRestricao;
    }

    public boolean isLeitura(Authentication autenticacao) {
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }
        if (autenticacao.getPrincipal() instanceof Usuario usuario) {
            return usuario.getRole() != null && usuario.getRole().isLeitura();
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN_LEITURA"::equals);
    }

    public void exigirNaoLeitura(Authentication autenticacao) {
        if (isLeitura(autenticacao)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de somente leitura não pode realizar alterações");
        }
    }

    public void exigirSuperAdmin(Authentication autenticacao, String adminToken) {
        if (!isSuperAdmin(autenticacao, adminToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao administrador mestre (SUPER_ADMIN)");
        }
    }

    public boolean isCliente(Authentication autenticacao) {
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CLIENTE"::equals);
    }

    public boolean podeAcessarSessao(Authentication autenticacao, String sessao) {
        if (isAdmin(autenticacao, null)) {
            return true;
        }
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Usuario usuario)) {
            return false;
        }
        return usuario.getRole() == Role.CLIENTE
                && usuario.getEmpresa() != null
                && sessao != null
                && sessao.equals(usuario.getEmpresa().getSessaoWhatsapp());
    }

    public Empresa validarAcessoSessao(Authentication autenticacao, String adminToken, String sessao) {
        boolean admin = isAdmin(autenticacao, adminToken);
        boolean cliente = isCliente(autenticacao);

        if (!admin && !cliente) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido");
        }

        if (admin) {
            Empresa empresa = buscarEmpresa(sessao);
            if (empresa == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para a sessão informada");
            }
            return empresa;
        }

        if (autenticacao.getPrincipal() instanceof Usuario usuario
                && usuario.getRole() == Role.CLIENTE
                && usuario.getEmpresa() != null
                && sessao != null
                && sessao.equals(usuario.getEmpresa().getSessaoWhatsapp())) {
            Empresa empresa = buscarEmpresa(sessao);
            if (empresa == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta empresa");
            }
            return empresa;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta empresa");
    }

    private Empresa buscarEmpresa(String sessao) {
        Empresa empresa = empresaRepository.buscarPorSessaoComModulos(sessao);
        if (empresa == null) {
            empresa = empresaRepository.findBySessaoWhatsapp(sessao);
        }
        if (empresa != null && Boolean.FALSE.equals(empresa.getAtivo())) {
            return null;
        }
        return empresa;
    }

    public Empresa validarAcessoExclusivoCliente(Authentication autenticacao, String adminToken, String sessao) {
        if (isAdmin(autenticacao, adminToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin nao tem acesso aos dados de reservas e contatos dos clientes");
        }
        if (!isCliente(autenticacao) || !(autenticacao.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Invalido");
        }
        Empresa empresa = buscarEmpresa(sessao);
        if (empresa == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada para a sessao informada");
        }
        if (usuario.getEmpresa() == null || !sessao.equals(usuario.getEmpresa().getSessaoWhatsapp())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta empresa");
        }
        return empresa;
    }
}
