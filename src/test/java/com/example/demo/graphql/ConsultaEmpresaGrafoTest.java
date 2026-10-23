package com.example.demo.graphql;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EmpresaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaEmpresaGrafoTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private ConsultaEmpresaGrafo consulta;

    @AfterEach
    void limparSessao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminListaNomeEPrecoDeTodas() {
        autenticarAdmin();
        when(empresaRepository.findAll()).thenReturn(List.of(
                empresa(1L, "Recanto", "Diária R$ 500"),
                empresa(2L, "Sítio", "Hora R$ 80")
        ));

        List<EmpresaConsulta> lista = consulta.listar();

        assertEquals(2, lista.size());
        assertEquals("Recanto", lista.get(0).nome());
        assertEquals("Diária R$ 500", lista.get(0).tabelaDePrecos());
        assertEquals(List.of("https://cdn/foto.jpg"), lista.get(0).urlsGaleria());
    }

    @Test
    void clienteSoLeAPropriaEmpresa() {
        Empresa propria = empresa(7L, "Minha", "Diária R$ 100");
        autenticarCliente(propria);
        when(empresaRepository.findById(7L)).thenReturn(Optional.of(propria));
        when(empresaRepository.findById(8L)).thenReturn(Optional.of(empresa(8L, "Outra", "Segredo")));

        assertEquals("Minha", consulta.buscarPorId("7").nome());
        assertNull(consulta.buscarPorId("8"));
        assertEquals("Diária R$ 100", consulta.listar().get(0).tabelaDePrecos());
        verify(empresaRepository, never()).findAll();
    }

    @Test
    void tokenDeClienteSemEmpresaNaoListaNada() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cliente-token",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        ));

        assertEquals(List.of(), consulta.listar());
        verifyNoInteractions(empresaRepository);
    }

    @Test
    void idInvalidoOuSessaoEmBrancoNaoConsultaOBanco() {
        autenticarAdmin();

        assertNull(consulta.buscarPorId("abc"));
        assertNull(consulta.buscarPorSessao("  "));
        verifyNoInteractions(empresaRepository);
    }

    private static Empresa empresa(Long id, String nome, String precos) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNome(nome);
        empresa.setTabelaDePrecos(precos);
        empresa.setUrlsGaleria(List.of("https://cdn/foto.jpg"));
        return empresa;
    }

    private static void autenticarAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin-api-key",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    private static void autenticarCliente(Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setRole(Role.CLIENTE);
        usuario.setEmpresa(empresa);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }
}
