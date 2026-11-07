package com.example.demo;

import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EmpresaRepositoryTest {

    @Autowired
    private EmpresaRepository repository;

    @Test
    @DisplayName("Salva e busca empresa por sessão WhatsApp no repositório")
    void deveSalvarEBuscarEmpresaComSucesso() {
        Empresa novaEmpresa = new Empresa();
        novaEmpresa.setNome("Cliente Teste SaaS");
        novaEmpresa.setSessaoWhatsapp("ClienteBotUnicoRepo");
        novaEmpresa.setMensagemSaudacao("Bem-vindo ao Cliente Teste!");
        novaEmpresa.setRamoDeAtuacao("LOCACAO");

        repository.save(novaEmpresa);
        Empresa empresaEncontrada = repository.findBySessaoWhatsapp("ClienteBotUnicoRepo");

        assertNotNull(empresaEncontrada);
        assertEquals("Cliente Teste SaaS", empresaEncontrada.getNome());
        assertEquals("LOCACAO", empresaEncontrada.getRamoDeAtuacao());
    }

    @Test
    @DisplayName("listarComModulos inclui empresas ativas e omite empresas com ativo=false")
    void deveOmitirEmpresasInativasEmListarComModulos() {
        Empresa ativa = new Empresa();
        ativa.setNome("Empresa Ativa Repo");
        ativa.setSessaoWhatsapp("sessao_ativa_repo_test");
        ativa.setAtivo(true);
        repository.save(ativa);

        Empresa inativa = new Empresa();
        inativa.setNome("Empresa Inativa Repo");
        inativa.setSessaoWhatsapp("sessao_inativa_repo_test");
        inativa.setAtivo(false);
        repository.save(inativa);

        List<Empresa> listadas = repository.listarComModulos();

        assertTrue(listadas.stream().anyMatch(e -> "sessao_ativa_repo_test".equals(e.getSessaoWhatsapp())));
        assertFalse(listadas.stream().anyMatch(e -> "sessao_inativa_repo_test".equals(e.getSessaoWhatsapp())));
    }
}
