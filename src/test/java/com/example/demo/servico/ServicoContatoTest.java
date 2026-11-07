package com.example.demo.servico;

import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.OrigemContato;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.util.CriptografiaCampo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoContatoTest {

    private static final String CHAVE_PADRAO = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String INDICE_PADRAO = "chave-teste-hmac-indice-32-bytes!";

    @Mock private ContatoRepository contatoRepository;
    @InjectMocks private ServicoContato servicoContato;

    private Empresa empresa;

    @BeforeEach
    void preparar() {
        CriptografiaCampo.configurarChaves(CHAVE_PADRAO, INDICE_PADRAO);
        empresa = new Empresa();
        empresa.setId(5L);
    }

    @AfterEach
    void restaurarChaves() {
        CriptografiaCampo.configurarChaves(CHAVE_PADRAO, INDICE_PADRAO);
    }

    @Test
    @DisplayName("Retorna nulo sem tocar no banco quando chaves de criptografia não estão configuradas")
    void deveRetornarNuloQuandoCriptografiaDesconfigurada() {
        CriptografiaCampo.configurarChaves(null, null);

        Contato resultado = servicoContato.registrarOuAtualizarContato(empresa, "5511988887777@c.us");

        assertNull(resultado);
        verifyNoInteractions(contatoRepository);
    }

    @Test
    @DisplayName("Cria novo contato com origem CONVERSA, finalTelefone de 4 dígitos e timestamps preenchidos")
    void deveCriarNovoContatoEmPrimeiraConversa() {
        when(contatoRepository.findByEmpresaIdAndNumeroIndice(eq(5L), anyString())).thenReturn(Optional.empty());
        when(contatoRepository.save(any(Contato.class))).thenAnswer(i -> i.getArgument(0));

        Contato salvo = servicoContato.registrarOuAtualizarContato(empresa, "5511988887777@c.us");

        assertNotNull(salvo);
        assertEquals("7777", salvo.getFinalTelefone());
        assertEquals(OrigemContato.CONVERSA, salvo.getOrigem());
        assertFalse(salvo.isNaoContatar());
        assertNotNull(salvo.getPrimeiraConversaEm());
        assertNotNull(salvo.getUltimaConversaEm());
    }

    @Test
    @DisplayName("Preserva primeiraConversaEm existente e atualiza ultimaConversaEm ao receber nova mensagem")
    void devePreservarPrimeiraConversaEmContatoExistente() {
        LocalDateTime antiga = LocalDateTime.of(2025, 5, 1, 9, 30);
        Contato existente = new Contato();
        existente.setEmpresa(empresa);
        existente.setPrimeiraConversaEm(antiga);

        when(contatoRepository.findByEmpresaIdAndNumeroIndice(eq(5L), anyString())).thenReturn(Optional.of(existente));
        when(contatoRepository.save(any(Contato.class))).thenAnswer(i -> i.getArgument(0));

        Contato atualizado = servicoContato.registrarOuAtualizarContato(empresa, "1853365294695@lid");

        assertEquals(antiga, atualizado.getPrimeiraConversaEm());
        assertNotNull(atualizado.getUltimaConversaEm());
    }
}
