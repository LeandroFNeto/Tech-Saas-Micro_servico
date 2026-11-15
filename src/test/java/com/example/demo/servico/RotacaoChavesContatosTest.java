package com.example.demo.servico;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RotacaoChavesContatosTest {

    private static final String CRIPTO_PADRAO_TESTE = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String INDICE_PADRAO_TESTE = "chave-teste-hmac-indice-32-bytes!";
    private static final String CRIPTO_ANTIGA = b64("antiga-0123456789-0123456789-abcd");
    private static final String INDICE_ANTIGO = "indice-antigo-0123456789-abcdefghij";
    private static final String CRIPTO_NOVA = b64("nova--0123456789-0123456789-abcdef");
    private static final String INDICE_NOVO = "indice-novo-0123456789-abcdefghijkl";
    private static final String CRIPTO_OUTRA = b64("outra-0123456789-0123456789-abcde");

    private static final String NUMERO_UM = "5511999998888";
    private static final String NUMERO_DOIS = "5521988887777";
    private static final String NUMERO_TRES = "5531977776666";
    private static final String NOME_UM = "Cliente Ficticio Um";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager gerenteDeTransacao;
    @Autowired private EmpresaRepository empresaRepository;

    private final ListAppender<ILoggingEvent> captura = new ListAppender<>();
    private final Logger raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private long empresaId;

    private static String b64(String texto) {
        return Base64.getEncoder().encodeToString((texto + "0".repeat(32)).substring(0, 32).getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void preparar() {
        CriptografiaCampo.configurarChaves(CRIPTO_NOVA, INDICE_NOVO);
        Empresa empresa = new Empresa();
        empresa.setNome("Empresa Rotacao");
        empresa.setSessaoWhatsapp("sessao_rotacao_" + System.nanoTime());
        empresaId = empresaRepository.save(empresa).getId();
        captura.start();
        raiz.addAppender(captura);
    }

    @AfterEach
    void limpar() {
        raiz.detachAppender(captura);
        jdbc.update("delete from reservas where empresa_id = ?", empresaId);
        jdbc.update("delete from contatos where empresa_id = ?", empresaId);
        empresaRepository.deleteById(empresaId);
        CriptografiaCampo.configurarChaves(CRIPTO_PADRAO_TESTE, INDICE_PADRAO_TESTE);
    }

    private RotacaoChavesContatos rotacao(String criptoAntiga, String indiceAntigo) {
        return new RotacaoChavesContatos(jdbc, gerenteDeTransacao, CRIPTO_NOVA, INDICE_NOVO, criptoAntiga, indiceAntigo);
    }

    private String cifrarComAntiga(String texto) {
        CriptografiaCampo.configurarChaves(CRIPTO_ANTIGA, INDICE_ANTIGO);
        try {
            return CriptografiaCampo.criptografar(texto);
        } finally {
            CriptografiaCampo.configurarChaves(CRIPTO_NOVA, INDICE_NOVO);
        }
    }

    private String indiceAntigoDe(String numero) {
        CriptografiaCampo.configurarChaves(CRIPTO_ANTIGA, INDICE_ANTIGO);
        try {
            return CriptografiaCampo.calcularIndice(WhatsappUtil.normalizarParaChaveEstado(numero));
        } finally {
            CriptografiaCampo.configurarChaves(CRIPTO_NOVA, INDICE_NOVO);
        }
    }

    private long inserirContato(String whatsappCifrado, String nomeCifrado, String indice) {
        jdbc.update("insert into contatos (nao_contatar, empresa_id, origem, numero_indice, whatsapp_id, nome) values (false, ?, 'CONVERSA', ?, ?, ?)",
                empresaId, indice, whatsappCifrado, nomeCifrado);
        return jdbc.queryForObject("select max(id) from contatos where empresa_id = ?", Long.class, empresaId);
    }

    private long inserirReserva(String tituloCifrado) {
        jdbc.update("insert into reservas (data, tentativas_envio, empresa_id, origem, situacao, titulo) values (current_date, 0, ?, 'BOT', 'SEM_CONTATO', ?)",
                empresaId, tituloCifrado);
        return jdbc.queryForObject("select max(id) from reservas where empresa_id = ?", Long.class, empresaId);
    }

    private String coluna(String tabela, String coluna, long id) {
        return jdbc.queryForObject("select " + coluna + " from " + tabela + " where id = ?", String.class, id);
    }

    private List<String> mensagensDoLog() {
        return captura.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    @Test
    @DisplayName("Rotaciona contatos e reservas, recalcula o indice e a busca pelo indice novo acha o contato")
    void rotacaoCompleta() {
        long um = inserirContato(cifrarComAntiga(NUMERO_UM), cifrarComAntiga(NOME_UM), indiceAntigoDe(NUMERO_UM));
        long dois = inserirContato(cifrarComAntiga(NUMERO_DOIS), null, indiceAntigoDe(NUMERO_DOIS));
        long reserva = inserirReserva(cifrarComAntiga("Reserva do Cliente Um"));

        rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar();

        assertEquals(NUMERO_UM, CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "whatsapp_id", um), CRIPTO_NOVA));
        assertEquals(NOME_UM, CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "nome", um), CRIPTO_NOVA));
        assertEquals(NUMERO_DOIS, CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "whatsapp_id", dois), CRIPTO_NOVA));
        assertNull(coluna("contatos", "nome", dois));
        assertEquals("Reserva do Cliente Um",
                CriptografiaCampo.tentarDescriptografarCom(coluna("reservas", "titulo", reserva), CRIPTO_NOVA));
        assertNull(CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "whatsapp_id", um), CRIPTO_ANTIGA));

        String indiceNovo = CriptografiaCampo.calcularIndice(WhatsappUtil.normalizarParaChaveEstado(NUMERO_UM));
        assertEquals(indiceNovo, coluna("contatos", "numero_indice", um));
        assertNotEquals(indiceAntigoDe(NUMERO_UM), coluna("contatos", "numero_indice", um));
        Long achado = jdbc.queryForObject("select id from contatos where empresa_id = ? and numero_indice = ?", Long.class, empresaId, indiceNovo);
        assertEquals(um, achado);
    }

    @Test
    @DisplayName("Valor gravado antes da cifra (sem v1:) e cifrado com a chave nova e o indice e recalculado")
    void valorSemPrefixo() {
        long id = inserirContato(NUMERO_TRES, "Nome Legado", "indice-qualquer");

        rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar();

        String whatsapp = coluna("contatos", "whatsapp_id", id);
        assertTrue(whatsapp.startsWith("v1:"));
        assertEquals(NUMERO_TRES, CriptografiaCampo.tentarDescriptografarCom(whatsapp, CRIPTO_NOVA));
        assertEquals("Nome Legado", CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "nome", id), CRIPTO_NOVA));
        assertEquals(CriptografiaCampo.calcularIndice(WhatsappUtil.normalizarParaChaveEstado(NUMERO_TRES)),
                coluna("contatos", "numero_indice", id));
    }

    @Test
    @DisplayName("Retomada: linhas ja rotacionadas ficam como estao e as demais sao rotacionadas")
    void retomadaDepoisDeFalha() {
        long primeira = inserirContato(cifrarComAntiga(NUMERO_UM), null, indiceAntigoDe(NUMERO_UM));
        rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar();
        String whatsappRotacionado = coluna("contatos", "whatsapp_id", primeira);
        String indiceRotacionado = coluna("contatos", "numero_indice", primeira);

        long segunda = inserirContato(cifrarComAntiga(NUMERO_DOIS), null, indiceAntigoDe(NUMERO_DOIS));
        rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar();

        assertEquals(whatsappRotacionado, coluna("contatos", "whatsapp_id", primeira));
        assertEquals(indiceRotacionado, coluna("contatos", "numero_indice", primeira));
        assertEquals(NUMERO_DOIS, CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "whatsapp_id", segunda), CRIPTO_NOVA));
    }

    @Test
    @DisplayName("Valor que nao abre com nenhuma chave interrompe a subida e o log traz so tabela e id")
    void chaveErradaInterrompe() {
        long boa = inserirContato(cifrarComAntiga(NUMERO_UM), null, indiceAntigoDe(NUMERO_UM));
        CriptografiaCampo.configurarChaves(CRIPTO_OUTRA, INDICE_NOVO);
        String deOutraChave = CriptografiaCampo.criptografar(NUMERO_DOIS);
        CriptografiaCampo.configurarChaves(CRIPTO_NOVA, INDICE_NOVO);
        long ruim = inserirContato(deOutraChave, null, "indice-ruim");

        RotacaoChavesContatos.RotacaoInterrompida erro = assertThrows(RotacaoChavesContatos.RotacaoInterrompida.class,
                () -> rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar());

        assertTrue(erro.getMessage().contains("tabela=contatos"));
        assertTrue(erro.getMessage().contains("id=" + ruim));
        assertFalse(erro.getMessage().contains(NUMERO_DOIS));
        assertFalse(erro.getMessage().contains(deOutraChave));
        assertEquals(deOutraChave, coluna("contatos", "whatsapp_id", ruim));
        assertEquals(NUMERO_UM, CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "whatsapp_id", boa), CRIPTO_NOVA));
        assertTrue(mensagensDoLog().stream().noneMatch(m -> m.contains(NUMERO_DOIS) || m.contains(deOutraChave)));
    }

    @Test
    @DisplayName("Configuracao incompleta, invalida ou igual a atual nao sobe e nao altera linha")
    void configuracaoRecusada() {
        String cifrado = cifrarComAntiga(NUMERO_UM);
        long id = inserirContato(cifrado, null, "indice-original");

        assertThrows(IllegalStateException.class, () -> rotacao(CRIPTO_ANTIGA, "").executar());
        assertThrows(IllegalStateException.class, () -> rotacao("", INDICE_ANTIGO).executar());
        assertThrows(IllegalStateException.class, () -> rotacao("curta", INDICE_ANTIGO).executar());
        assertThrows(IllegalStateException.class, () -> rotacao(CRIPTO_NOVA, INDICE_ANTIGO).executar());
        assertThrows(IllegalStateException.class, () -> rotacao(CRIPTO_ANTIGA, INDICE_NOVO).executar());

        assertEquals(cifrado, coluna("contatos", "whatsapp_id", id));
        assertEquals("indice-original", coluna("contatos", "numero_indice", id));
    }

    @Test
    @DisplayName("Sem chaves antigas, nenhuma linha e lida nem alterada")
    void semChaveAntiga() {
        long id = inserirContato("v1:lixo-que-nao-abre", null, "indice-original");

        rotacao("", "").executar();

        assertEquals("v1:lixo-que-nao-abre", coluna("contatos", "whatsapp_id", id));
        assertTrue(mensagensDoLog().stream().noneMatch(m -> m.contains("Rotacao")));
    }

    @Test
    @DisplayName("Linha sem whatsapp_id recebe indice aleatorio no formato do indice, mantem nao_contatar e entra no log so como quantidade")
    void linhaSemNumero() {
        long id = inserirContato(null, null, "a".repeat(64));
        jdbc.update("update contatos set nao_contatar = true where id = ?", id);
        long normal = inserirContato(cifrarComAntiga(NUMERO_UM), null, indiceAntigoDe(NUMERO_UM));

        rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar();

        String indice = coluna("contatos", "numero_indice", id);
        assertTrue(indice.matches("[0-9a-f]{64}"));
        assertNotEquals("a".repeat(64), indice);
        assertNull(coluna("contatos", "whatsapp_id", id));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("select nao_contatar from contatos where id = ?", Boolean.class, id));
        assertEquals(NUMERO_UM, CriptografiaCampo.tentarDescriptografarCom(coluna("contatos", "whatsapp_id", normal), CRIPTO_NOVA));

        List<String> mensagens = mensagensDoLog().stream().filter(m -> m.contains("Rotacao")).collect(Collectors.toList());
        assertEquals(1, mensagens.size());
        assertTrue(mensagens.get(0).contains("contatos sem numero=1"));
        assertTrue(mensagens.get(0).contains("contatos rotacionados=1"));
    }

    @Test
    @DisplayName("O log final traz so quantidades por tabela, sem numero, nome, titulo nem chave")
    void logSoComQuantidades() {
        inserirContato(cifrarComAntiga(NUMERO_UM), cifrarComAntiga(NOME_UM), indiceAntigoDe(NUMERO_UM));
        inserirReserva(cifrarComAntiga("Titulo Secreto"));

        rotacao(CRIPTO_ANTIGA, INDICE_ANTIGO).executar();

        List<String> mensagens = mensagensDoLog().stream().filter(m -> m.contains("Rotacao")).collect(Collectors.toList());
        assertEquals(1, mensagens.size());
        String linha = mensagens.get(0);
        assertTrue(linha.contains("contatos rotacionados=1"));
        assertTrue(linha.contains("reservas rotacionadas=1"));
        for (String proibido : List.of(NUMERO_UM, NOME_UM, "Titulo Secreto", CRIPTO_ANTIGA, CRIPTO_NOVA, INDICE_ANTIGO, INDICE_NOVO)) {
            assertFalse(linha.contains(proibido));
        }
        assertTrue(captura.list.stream().noneMatch(e -> e.getLevel() == Level.ERROR));
    }
}
