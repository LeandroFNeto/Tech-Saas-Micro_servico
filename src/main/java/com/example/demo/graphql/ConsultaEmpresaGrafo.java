package com.example.demo.graphql;

import com.example.demo.model.Empresa;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Leitura enxuta de empresa para a IA. Reusa o repositório já existente
 * e devolve um registro próprio, para o GraphQL serializar só os campos pedidos.
 */
@Service
public class ConsultaEmpresaGrafo {

    private final EmpresaRepository empresaRepository;

    public ConsultaEmpresaGrafo(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<EmpresaConsulta> listar() {
        if (isAdmin()) {
            return empresaRepository.findAll().stream().map(ConsultaEmpresaGrafo::paraConsulta).toList();
        }
        Long empresaId = empresaIdDoCliente();
        if (empresaId == null) {
            return List.of();
        }
        return empresaRepository.findById(empresaId)
                .map(ConsultaEmpresaGrafo::paraConsulta)
                .map(List::of)
                .orElseGet(List::of);
    }

    public EmpresaConsulta buscarPorId(String id) {
        Long empresaId = idNumerico(id);
        if (empresaId == null) {
            return null;
        }
        return lerSePermitido(empresaRepository.findById(empresaId).orElse(null));
    }

    public EmpresaConsulta buscarPorSessao(String sessaoWhatsapp) {
        if (sessaoWhatsapp == null || sessaoWhatsapp.isBlank()) {
            return null;
        }
        return lerSePermitido(empresaRepository.findBySessaoWhatsapp(sessaoWhatsapp.trim()));
    }

    private EmpresaConsulta lerSePermitido(Empresa empresa) {
        if (empresa == null || !podeLer(empresa)) {
            return null;
        }
        return paraConsulta(empresa);
    }

    private boolean podeLer(Empresa empresa) {
        if (isAdmin()) {
            return true;
        }
        Long empresaId = empresaIdDoCliente();
        return empresaId != null && empresaId.equals(empresa.getId());
    }

    private boolean isAdmin() {
        Authentication autenticacao = autenticacaoAtual();
        if (autenticacao == null) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .anyMatch(autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()));
    }

    private Long empresaIdDoCliente() {
        Authentication autenticacao = autenticacaoAtual();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Usuario usuario)) {
            return null;
        }
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            return null;
        }
        return empresa.getId();
    }

    private Authentication autenticacaoAtual() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static Long idNumerico(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static EmpresaConsulta paraConsulta(Empresa empresa) {
        List<String> urls = empresa.getUrlsGaleria().stream()
                .filter(Objects::nonNull)
                .toList();
        return new EmpresaConsulta(
                empresa.getId() == null ? null : String.valueOf(empresa.getId()),
                empresa.getNome(),
                empresa.getTabelaDePrecos(),
                empresa.getRamoDeAtuacao(),
                empresa.getRegrasLocacao(),
                empresa.getMensagemSaudacao(),
                empresa.getSessaoWhatsapp(),
                empresa.getLinkGoogleMaps(),
                empresa.getLinkFotoPrincipal(),
                empresa.getLinkGaleria(),
                urls,
                empresa.getUsaIA(),
                empresa.getLocacaoPorHora(),
                empresa.getPermiteReservaAutomatica()
        );
    }
}
