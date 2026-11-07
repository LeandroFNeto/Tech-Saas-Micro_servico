package com.example.demo.servico;

import com.example.demo.dto.contato.ContatoResumoDTO;
import com.example.demo.dto.contato.NumeroContatoDTO;
import com.example.demo.model.AuditoriaAcessoContato;
import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.OrigemContato;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
public class ServicoContato {

    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final int LIMITE_REVELACOES_POR_HORA = 10;
    private static final int TAMANHO_PAGINA_PADRAO = 10;
    private static final int TAMANHO_PAGINA_MAXIMO = 50;
    private static final Set<String> COMANDOS_OPT_OUT = Set.of(
            "sair", "cancelar", "stop", "pare", "parar", "descadastrar", "remover"
    );

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private AuditoriaAcessoContatoRepository auditoriaAcessoContatoRepository;

    @Transactional
    public Contato registrarOuAtualizarContato(Empresa empresa, String whatsappId) {
        return registrarOuAtualizarContato(empresa, whatsappId, null);
    }

    @Transactional
    public Contato registrarOuAtualizarContato(Empresa empresa, String whatsappId, String nome) {
        if (!CriptografiaCampo.isConfigurado()) {
            CriptografiaCampo.avisarChavesNaoConfiguradasSeNecessario();
            return null;
        }

        String numeroChave = WhatsappUtil.normalizarParaChaveEstado(whatsappId);
        String numeroIndice = CriptografiaCampo.calcularIndice(numeroChave);
        String digitos = WhatsappUtil.extrairApenasNumeros(whatsappId);
        String finalTelefone = digitos.length() >= 4 ? digitos.substring(digitos.length() - 4) : digitos;

        LocalDateTime agora = LocalDateTime.now(FUSO_BRASILIA);

        Contato contato = contatoRepository.findByEmpresaIdAndNumeroIndice(empresa.getId(), numeroIndice)
                .orElseGet(() -> {
                    Contato novo = new Contato();
                    novo.setEmpresa(empresa);
                    novo.setWhatsappId(whatsappId);
                    novo.setNumeroIndice(numeroIndice);
                    novo.setFinalTelefone(finalTelefone);
                    novo.setPrimeiraConversaEm(agora);
                    novo.setNaoContatar(false);
                    novo.setOrigem(OrigemContato.CONVERSA);
                    return novo;
                });

        if (nome != null && !nome.isBlank()) {
            contato.setNome(nome.trim());
        }
        if (contato.getPrimeiraConversaEm() == null) {
            contato.setPrimeiraConversaEm(agora);
        }
        contato.setUltimaConversaEm(agora);
        return contatoRepository.save(contato);
    }

    @Transactional(readOnly = true)
    public List<ContatoResumoDTO> listarContatos(Empresa empresa, int pagina, int tamanho) {
        int pag = Math.max(0, pagina);
        int tam = tamanho <= 0 ? TAMANHO_PAGINA_PADRAO : Math.min(tamanho, TAMANHO_PAGINA_MAXIMO);

        Page<Contato> paginaContatos = contatoRepository.findByEmpresaIdOrderByUltimaConversaEmDesc(
                empresa.getId(),
                PageRequest.of(pag, tam)
        );

        return paginaContatos.getContent().stream()
                .map(ContatoResumoDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarRevelacoesUltimaHora(Empresa empresa) {
        LocalDateTime limite = LocalDateTime.now(FUSO_BRASILIA).minusHours(1);
        return auditoriaAcessoContatoRepository.countByEmpresaIdAndDataHoraAfter(empresa.getId(), limite);
    }

    @Transactional
    public NumeroContatoDTO obterNumeroCompleto(Empresa empresa, Long contatoId, String emailUsuario) {
        long revelacoes = contarRevelacoesUltimaHora(empresa);
        if (revelacoes >= LIMITE_REVELACOES_POR_HORA) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de 10 visualizações de contatos por hora atingido");
        }

        Contato contato = contatoRepository.findByIdAndEmpresaId(contatoId, empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado"));

        auditoriaAcessoContatoRepository.save(new AuditoriaAcessoContato(
                empresa,
                contato,
                LocalDateTime.now(FUSO_BRASILIA),
                emailUsuario
        ));

        String numero = contato.getWhatsappId();
        if (numero != null && numero.contains("@")) {
            numero = numero.split("@")[0];
        }
        return new NumeroContatoDTO(numero);
    }

    @Transactional
    public void excluirContato(Empresa empresa, Long contatoId) {
        Contato contato = contatoRepository.findByIdAndEmpresaId(contatoId, empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado"));
        contatoRepository.delete(contato);
    }

    @Transactional
    public boolean processarOptOutPorMensagem(Empresa empresa, String rawFrom, String textoMensagem) {
        if (textoMensagem == null || rawFrom == null || !CriptografiaCampo.isConfigurado()) {
            return false;
        }

        String textoLimpo = textoMensagem.trim().toLowerCase();
        if (!COMANDOS_OPT_OUT.contains(textoLimpo)) {
            return false;
        }

        String numeroChave = WhatsappUtil.normalizarParaChaveEstado(rawFrom);
        String numeroIndice = CriptografiaCampo.calcularIndice(numeroChave);

        contatoRepository.findByEmpresaIdAndNumeroIndice(empresa.getId(), numeroIndice)
                .ifPresent(contato -> {
                    contato.setNaoContatar(true);
                    contatoRepository.save(contato);
                });

        return true;
    }
}
