package com.example.demo.servico;

import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.OrigemContato;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ServicoContato {

    @Autowired
    private ContatoRepository contatoRepository;

    @Transactional
    public Contato registrarOuAtualizarContato(Empresa empresa, String whatsappId) {
        if (!CriptografiaCampo.isConfigurado()) {
            CriptografiaCampo.avisarChavesNaoConfiguradasSeNecessario();
            return null;
        }

        String numeroChave = WhatsappUtil.normalizarParaChaveEstado(whatsappId);
        String numeroIndice = CriptografiaCampo.calcularIndice(numeroChave);
        String digitos = WhatsappUtil.extrairApenasNumeros(whatsappId);
        String finalTelefone = digitos.length() >= 4 ? digitos.substring(digitos.length() - 4) : digitos;

        LocalDateTime agora = LocalDateTime.now();

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

        if (contato.getPrimeiraConversaEm() == null) {
            contato.setPrimeiraConversaEm(agora);
        }
        contato.setUltimaConversaEm(agora);
        return contatoRepository.save(contato);
    }
}
