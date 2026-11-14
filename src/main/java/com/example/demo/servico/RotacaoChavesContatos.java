package com.example.demo.servico;

import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.List;

@Component
public class RotacaoChavesContatos implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(RotacaoChavesContatos.class);
    private static final String PREFIXO = "v1:";
    private static final int LOTE = 200;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final String chaveCriptoAtual;
    private final String chaveIndiceAtual;
    private final String chaveCriptoAntiga;
    private final String chaveIndiceAntiga;

    public RotacaoChavesContatos(
            JdbcTemplate jdbc,
            PlatformTransactionManager gerenteDeTransacao,
            @Value("${contatos.cripto.chave:}") String chaveCriptoAtual,
            @Value("${contatos.indice.chave:}") String chaveIndiceAtual,
            @Value("${contatos.cripto.chave-antiga:}") String chaveCriptoAntiga,
            @Value("${contatos.indice.chave-antiga:}") String chaveIndiceAntiga) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(gerenteDeTransacao);
        this.chaveCriptoAtual = limpar(chaveCriptoAtual);
        this.chaveIndiceAtual = limpar(chaveIndiceAtual);
        this.chaveCriptoAntiga = limpar(chaveCriptoAntiga);
        this.chaveIndiceAntiga = limpar(chaveIndiceAntiga);
    }

    @Override
    public void afterSingletonsInstantiated() {
        executar();
    }

    public void executar() {
        boolean temCripto = !chaveCriptoAntiga.isEmpty();
        boolean temIndice = !chaveIndiceAntiga.isEmpty();
        if (!temCripto && !temIndice) {
            return;
        }
        if (temCripto != temIndice) {
            throw new IllegalStateException("Rotacao de chaves de contatos: defina as duas variaveis antigas ou nenhuma.");
        }
        if (!CriptografiaCampo.chaveCriptoValida(chaveCriptoAntiga)) {
            throw new IllegalStateException("Rotacao de chaves de contatos: a chave de criptografia antiga e invalida.");
        }
        if (!CriptografiaCampo.isConfigurado() || !CriptografiaCampo.chaveCriptoValida(chaveCriptoAtual)
                || chaveIndiceAtual.isEmpty()) {
            throw new IllegalStateException("Rotacao de chaves de contatos: as chaves atuais nao estao configuradas.");
        }
        if (chaveCriptoAntiga.equals(chaveCriptoAtual) || chaveIndiceAntiga.equals(chaveIndiceAtual)) {
            throw new IllegalStateException("Rotacao de chaves de contatos: a chave antiga e igual a atual.");
        }

        int[] contatos = rotacionarContatos();
        int reservas = rotacionarReservas();
        log.info("Rotacao de chaves de contatos concluida: contatos rotacionados={}, contatos sem numero={}, reservas rotacionadas={}.",
                contatos[0], contatos[1], reservas);
    }

    private int[] rotacionarContatos() {
        int rotacionados = 0;
        int semNumero = 0;
        long ultimoId = 0;
        while (true) {
            List<LinhaContato> lote = jdbc.query(
                    "select id, whatsapp_id, nome, numero_indice from contatos where id > ? order by id limit " + LOTE,
                    (rs, n) -> new LinhaContato(rs.getLong("id"), rs.getString("whatsapp_id"),
                            rs.getString("nome"), rs.getString("numero_indice")),
                    ultimoId);
            if (lote.isEmpty()) {
                return new int[]{rotacionados, semNumero};
            }
            for (LinhaContato linha : lote) {
                ultimoId = linha.id();
                int resultado = rotacionarContato(linha);
                if (resultado == 1) {
                    rotacionados++;
                } else if (resultado == 2) {
                    semNumero++;
                }
            }
        }
    }

    private int rotacionarContato(LinhaContato linha) {
        try {
            if (linha.whatsappId() == null) {
                String nome = reciframento(linha.nome(), "contatos", linha.id()).novo();
                transacao.executeWithoutResult(status -> jdbc.update(
                        "update contatos set nome = ?, numero_indice = ? where id = ?",
                        nome, indiceAleatorio(), linha.id()));
                return 2;
            }
            Reciframento whatsapp = reciframento(linha.whatsappId(), "contatos", linha.id());
            Reciframento nome = reciframento(linha.nome(), "contatos", linha.id());
            if (!whatsapp.alterado() && !nome.alterado()) {
                return 0;
            }
            String indice = whatsapp.alterado()
                    ? CriptografiaCampo.calcularIndice(WhatsappUtil.normalizarParaChaveEstado(whatsapp.plano()))
                    : linha.numeroIndice();
            transacao.executeWithoutResult(status -> jdbc.update(
                    "update contatos set whatsapp_id = ?, nome = ?, numero_indice = ? where id = ?",
                    whatsapp.novo(), nome.novo(), indice, linha.id()));
            return 1;
        } catch (RotacaoInterrompida e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RotacaoInterrompida("contatos", linha.id());
        }
    }

    private int rotacionarReservas() {
        int rotacionadas = 0;
        long ultimoId = 0;
        while (true) {
            List<LinhaReserva> lote = jdbc.query(
                    "select id, titulo from reservas where id > ? order by id limit " + LOTE,
                    (rs, n) -> new LinhaReserva(rs.getLong("id"), rs.getString("titulo")),
                    ultimoId);
            if (lote.isEmpty()) {
                return rotacionadas;
            }
            for (LinhaReserva linha : lote) {
                ultimoId = linha.id();
                try {
                    Reciframento titulo = reciframento(linha.titulo(), "reservas", linha.id());
                    if (titulo.alterado()) {
                        transacao.executeWithoutResult(status -> jdbc.update(
                                "update reservas set titulo = ? where id = ?", titulo.novo(), linha.id()));
                        rotacionadas++;
                    }
                } catch (RotacaoInterrompida e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw new RotacaoInterrompida("reservas", linha.id());
                }
            }
        }
    }

    private Reciframento reciframento(String valor, String tabela, long id) {
        if (valor == null) {
            return new Reciframento(null, null, false);
        }
        if (!valor.startsWith(PREFIXO)) {
            return new Reciframento(valor, CriptografiaCampo.criptografar(valor), true);
        }
        String comNova = CriptografiaCampo.tentarDescriptografarCom(valor, chaveCriptoAtual);
        if (comNova != null) {
            return new Reciframento(comNova, valor, false);
        }
        String comAntiga = CriptografiaCampo.tentarDescriptografarCom(valor, chaveCriptoAntiga);
        if (comAntiga == null) {
            throw new RotacaoInterrompida(tabela, id);
        }
        return new Reciframento(comAntiga, CriptografiaCampo.criptografar(comAntiga), true);
    }

    private static String indiceAleatorio() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private record LinhaContato(long id, String whatsappId, String nome, String numeroIndice) {
    }

    private record LinhaReserva(long id, String titulo) {
    }

    private record Reciframento(String plano, String novo, boolean alterado) {
    }

    public static class RotacaoInterrompida extends IllegalStateException {

        public RotacaoInterrompida(String tabela, long id) {
            super("Rotacao de chaves de contatos interrompida: valor nao abre com nenhuma das chaves ou nao pode ser gravado. tabela=" + tabela + " id=" + id);
        }
    }
}
