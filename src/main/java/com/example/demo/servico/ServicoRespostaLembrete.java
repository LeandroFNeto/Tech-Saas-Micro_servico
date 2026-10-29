package com.example.demo.servico;

import com.example.demo.model.Contato;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.model.Reserva;
import com.example.demo.model.SituacaoReserva;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.util.CriptografiaCampo;
import com.example.demo.util.WhatsappUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ServicoRespostaLembrete {

    private static final String MENSAGEM_CONFIRMACAO =
            "Perfeito! Sua reserva está confirmada. Obrigado!";
    private static final String MENSAGEM_REMARCACAO =
            "Entendido! O responsável pelo espaço entrará em contato em breve para combinar com você.";
    private static final String MENSAGEM_PARAR =
            "Você não receberá mais lembretes automáticos de confirmação.";

    @Autowired
    private InterpretadorRespostaLembrete interpretador;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ServicoMensagem servicoMensagem;

    @Transactional
    public boolean processarPossivelResposta(Empresa empresa, String numeroWhatsapp, String texto, EstadoUsuario estadoAtual) {
        if (estadoAtual != EstadoUsuario.INICIO) {
            return false;
        }
        if (!CriptografiaCampo.isConfigurado()) {
            return false;
        }

        String numeroChave = WhatsappUtil.normalizarParaChaveEstado(numeroWhatsapp);
        String numeroIndice = CriptografiaCampo.calcularIndice(numeroChave);

        Contato contato = contatoRepository.findByEmpresaIdAndNumeroIndice(empresa.getId(), numeroIndice)
                .orElse(null);
        if (contato == null) {
            return false;
        }

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(fuso);
        LocalDateTime agora = LocalDateTime.now(fuso);

        List<Reserva> candidatos = reservaRepository.findByContatoIdAndSituacaoInAndDataGreaterThanEqualOrderByDataAsc(
                contato.getId(),
                List.of(SituacaoReserva.LEMBRETE_ENVIADO, SituacaoReserva.SEM_RESPOSTA),
                hoje
        );

        Reserva reservaAlvo = null;
        for (Reserva r : candidatos) {
            if (r.getLembreteEnviadoEm() != null && agora.isBefore(r.getLembreteEnviadoEm().plusHours(72))) {
                reservaAlvo = r;
                break;
            }
        }

        if (reservaAlvo == null) {
            return false;
        }

        IntencaoResposta intencao = interpretador.interpretar(texto);
        if (intencao == IntencaoResposta.OUTRO) {
            return false;
        }

        if (intencao == IntencaoResposta.CONFIRMA) {
            reservaAlvo.setSituacao(SituacaoReserva.CONFIRMADA_PELO_CLIENTE);
            reservaAlvo.setAtualizadaEm(agora);
            reservaRepository.save(reservaAlvo);
            servicoMensagem.enviarMensagemWPP(empresa, numeroWhatsapp, MENSAGEM_CONFIRMACAO);
            return true;
        }

        if (intencao == IntencaoResposta.REMARCA) {
            reservaAlvo.setSituacao(SituacaoReserva.CANCELAMENTO_SOLICITADO);
            reservaAlvo.setAtualizadaEm(agora);
            reservaRepository.save(reservaAlvo);
            servicoMensagem.enviarMensagemWPP(empresa, numeroWhatsapp, MENSAGEM_REMARCACAO);
            return true;
        }

        if (intencao == IntencaoResposta.PARAR) {
            contato.setNaoContatar(true);
            contato.setUltimaConversaEm(agora);
            contatoRepository.save(contato);
            servicoMensagem.enviarMensagemWPP(empresa, numeroWhatsapp, MENSAGEM_PARAR);
            return true;
        }

        return false;
    }
}
