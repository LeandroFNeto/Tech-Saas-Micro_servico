package com.example.demo.servico.assistente;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.model.Aviso;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.servico.ServicoIA;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssistenteTutoriaService {

    private final AvisoRepository avisoRepository;
    private final ServicoIA servicoIA;
    private volatile String conteudoBaseConhecimento;

    public AssistenteTutoriaService(AvisoRepository avisoRepository, ServicoIA servicoIA) {
        this.avisoRepository = avisoRepository;
        this.servicoIA = servicoIA;
    }

    public AssistenteChatResponseDTO responder(AssistenteChatRequestDTO requisicao) {
        String modo = (requisicao.modo() == null || requisicao.modo().isBlank())
                ? "DIAGNOSTICO"
                : requisicao.modo().toUpperCase().trim();

        String base = obterBaseConhecimento();
        List<Aviso> avisos = avisoRepository != null
                ? avisoRepository.findByAtivoTrueOrderByIdDesc()
                : List.of();

        String prompt = construirPrompt(requisicao.mensagem(), modo, base, avisos);
        String respostaIA = servicoIA != null ? servicoIA.gerarRespostaHumanizada(prompt) : null;

        String respostaFinal;
        if (respostaIA != null && !respostaIA.isBlank()) {
            respostaFinal = respostaIA.trim();
        } else {
            respostaFinal = gerarRespostaFallback(requisicao.mensagem(), modo, avisos);
        }

        List<String> sugestoes = obterSugestoes(modo);
        return new AssistenteChatResponseDTO(respostaFinal, modo, Instant.now(), sugestoes);
    }

    public String obterBaseConhecimento() {
        if (conteudoBaseConhecimento != null) {
            return conteudoBaseConhecimento;
        }

        try {
            File arquivo = new File("docs/base-conhecimento-gamb.md");
            if (arquivo.exists()) {
                conteudoBaseConhecimento = Files.readString(arquivo.toPath(), StandardCharsets.UTF_8);
                return conteudoBaseConhecimento;
            }
        } catch (Exception ignored) {
        }

        try {
            ClassPathResource recurso = new ClassPathResource("base-conhecimento-gamb.md");
            if (recurso.exists()) {
                try (InputStream is = recurso.getInputStream()) {
                    conteudoBaseConhecimento = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return conteudoBaseConhecimento;
                }
            }
        } catch (Exception ignored) {
        }

        conteudoBaseConhecimento = "GAMB: Plataforma SaaS com assistente virtual para WhatsApp e agendamentos com Google Agenda.";
        return conteudoBaseConhecimento;
    }

    private String construirPrompt(String mensagem, String modo, String base, List<Aviso> avisos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é o Assistente Especialista do sistema GAMB (Tech-SaaS).\n");
        sb.append("Base de Conhecimento Oficial do Sistema:\n");
        sb.append(base).append("\n\n");

        if (!avisos.isEmpty()) {
            sb.append("Avisos e Comunicados Ativos do Sistema:\n");
            for (Aviso a : avisos) {
                sb.append("- [").append(a.getTipo()).append("] ").append(a.getTitulo()).append(": ").append(a.getConteudo()).append("\n");
            }
            sb.append("\n");
        }

        if ("ATENDIMENTO_SANDBOX".equalsIgnoreCase(modo)) {
            sb.append("Modo de Operação: ATENDIMENTO_SANDBOX (Simulação de Atendimento ao Cliente)\n");
            sb.append("Instrução: Responda como o assistente virtual atende o cliente no WhatsApp, educado, tirando dúvidas de disponibilidade, regras, valores ou comunicados.\n");
        } else {
            sb.append("Modo de Operação: DIAGNOSTICO (Ajuda Interna e Configuração)\n");
            sb.append("Instrução: Explique com clareza como o sistema funciona, tire dúvidas técnicas/operacionais e ensine passo a passo a configurar qualquer recurso do GAMB.\n");
        }

        sb.append("Responda em Português do Brasil com clareza e precisão.\n");
        sb.append("Pergunta do Usuário: \"").append(mensagem.replace("\"", "'")).append("\"");
        return sb.toString();
    }

    private String gerarRespostaFallback(String mensagem, String modo, List<Aviso> avisos) {
        String msgLower = mensagem.toLowerCase();

        if (msgLower.contains("whatsapp") || msgLower.contains("qr") || msgLower.contains("conectar")) {
            return "Para conectar o WhatsApp: no Painel do GAMB, acesse a seção 'Conexão WhatsApp' em seu perfil, clique em 'Iniciar Conexão', aponte a câmera do WhatsApp para o QR Code em 'Aparelhos Conectados' e aguarde o status 'Conectado'.";
        }

        if (msgLower.contains("aviso") || msgLower.contains("comunicado")) {
            if (!avisos.isEmpty()) {
                Aviso primeiro = avisos.get(0);
                return "Existem avisos ativos no sistema. O mais recente é '" + primeiro.getTitulo() + "': " + primeiro.getConteudo();
            }
            return "O módulo de avisos permite que o administrador cadastre comunicados, tutoriais ou avisos de manutenção via tela de administração (/admin/avisos).";
        }

        if (msgLower.contains("reserva") || msgLower.contains("agenda") || msgLower.contains("horario") || msgLower.contains("configurar")) {
            return "As regras e modalidades de reserva podem ser configuradas no painel em 'Configurações do Estabelecimento'. É possível definir locação por diária completa ou por hora, integrando os agendamentos diretamente ao Google Agenda.";
        }

        if ("ATENDIMENTO_SANDBOX".equalsIgnoreCase(modo)) {
            return "Olá! Sou o assistente virtual do GAMB. Posso lhe orientar sobre fotos do local, regras de locação, valores ou consultar disponibilidade de datas na agenda. Como posso ajudar?";
        }

        return "Assistente GAMB: Plataforma operacional com microserviços de atendimento automatizado via WhatsApp e reservas no Google Agenda. Na tela de admin você pode usar o botão de diagnóstico para checar a saúde dos módulos ou configurar novas empresas.";
    }

    private List<String> obterSugestoes(String modo) {
        if ("ATENDIMENTO_SANDBOX".equalsIgnoreCase(modo)) {
            return List.of(
                    "Quais são os horários e regras de locação?",
                    "Como funciona a reserva integrada à agenda?",
                    "Tem algum aviso ativo no momento?"
            );
        }
        return List.of(
                "Como conectar o WhatsApp via QR Code?",
                "Como configurar locação por hora vs diária?",
                "Como cadastrar avisos e tutoriais no painel?",
                "Como funciona o diagnóstico geral dos módulos?"
        );
    }
}
