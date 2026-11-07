package com.example.demo.servico;

import com.example.demo.dto.contato.ContatoResumoDTO;
import com.example.demo.dto.contato.NumeroContatoDTO;
import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.OrigemContato;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoContatoTest {

    private static final String CHAVE_PADRAO = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String INDICE_PADRAO = "chave-teste-hmac-indice-32-bytes!";

    @Mock private ContatoRepository contatoRepository;
    @Mock private AuditoriaAcessoContatoRepository auditoriaAcessoContatoRepository;
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

    @Test
    @DisplayName("Salva o nome do remetente informado no contato")
    void deveSalvarNomeQuandoInformado() {
        when(contatoRepository.findByEmpresaIdAndNumeroIndice(eq(5L), anyString())).thenReturn(Optional.empty());
        when(contatoRepository.save(any(Contato.class))).thenAnswer(i -> i.getArgument(0));

        Contato salvo = servicoContato.registrarOuAtualizarContato(empresa, "5511999998888@c.us", "Maria Santos");

        assertNotNull(salvo);
        assertEquals("Maria Santos", salvo.getNome());
    }

    @Test
    @DisplayName("Lista contatos paginados com número mascarado")
    void deveListarContatosPaginadosComMascara() {
        Contato c = new Contato();
        c.setId(10L);
        c.setNome("Carlos Silva");
        c.setFinalTelefone("1234");
        c.setPrimeiraConversaEm(LocalDateTime.now());
        c.setUltimaConversaEm(LocalDateTime.now());

        when(contatoRepository.findByEmpresaIdOrderByUltimaConversaEmDesc(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(c)));

        List<ContatoResumoDTO> resultado = servicoContato.listarContatos(empresa, 0, 10);

        assertEquals(1, resultado.size());
        assertEquals("Carlos Silva", resultado.get(0).nome());
        assertEquals("(**) *****-1234", resultado.get(0).telefoneMascarado());
    }

    @Test
    @DisplayName("Permite revelar número completo quando dentro da cota de 10 por hora")
    void devePermitirRevelarNumeroDentroDaCota() {
        when(auditoriaAcessoContatoRepository.countByEmpresaIdAndDataHoraAfter(eq(5L), any(LocalDateTime.class)))
                .thenReturn(3L);

        Contato c = new Contato();
        c.setId(20L);
        c.setWhatsappId("5511987654321@c.us");
        when(contatoRepository.findByIdAndEmpresaId(20L, 5L)).thenReturn(Optional.of(c));

        NumeroContatoDTO dto = servicoContato.obterNumeroCompleto(empresa, 20L, "operador@teste.com");

        assertNotNull(dto);
        assertEquals("5511987654321", dto.numero());
        verify(auditoriaAcessoContatoRepository).save(any());
    }

    @Test
    @DisplayName("Bloqueia com 429 Too Many Requests ao tentar revelar o 11º contato na mesma hora")
    void deveBloquearCom429QuandoAtingirLimiteDe10RevelacoesPorHora() {
        when(auditoriaAcessoContatoRepository.countByEmpresaIdAndDataHoraAfter(eq(5L), any(LocalDateTime.class)))
                .thenReturn(10L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                servicoContato.obterNumeroCompleto(empresa, 20L, "operador@teste.com")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(contatoRepository, never()).findByIdAndEmpresaId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Exclui contato definitivamente da base")
    void deveExcluirContato() {
        Contato c = new Contato();
        c.setId(30L);
        when(contatoRepository.findByIdAndEmpresaId(30L, 5L)).thenReturn(Optional.of(c));

        servicoContato.excluirContato(empresa, 30L);

        verify(contatoRepository).delete(c);
    }

    @Test
    @DisplayName("Processa opt-out quando cliente envia mensagem SAIR ou CANCELAR")
    void deveProcessarOptOutQuandoClienteEnviaComando() {
        Contato c = new Contato();
        c.setId(40L);
        c.setNaoContatar(false);

        when(contatoRepository.findByEmpresaIdAndNumeroIndice(eq(5L), anyString())).thenReturn(Optional.of(c));

        boolean tratado = servicoContato.processarOptOutPorMensagem(empresa, "5511988887777@c.us", "SAIR");

        assertTrue(tratado);
        assertTrue(c.isNaoContatar());
        verify(contatoRepository).save(c);
    }

    @Test
    @DisplayName("Ignora mensagem comum sem disparar opt-out")
    void deveIgnorarMensagemComumSemDispararOptOut() {
        boolean tratado = servicoContato.processarOptOutPorMensagem(empresa, "5511988887777@c.us", "Olá, gostaria de reservar");

        assertFalse(tratado);
        verify(contatoRepository, never()).findByEmpresaIdAndNumeroIndice(anyLong(), anyString());
    }
}
