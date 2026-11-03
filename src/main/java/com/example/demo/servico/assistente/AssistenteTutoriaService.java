package com.example.demo.servico.assistente;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.model.Aviso;
import com.example.demo.model.Empresa;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoMenu;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ServicoMenu servicoMenu;
    private volatile String conteudoBaseConhecimento;

    @Autowired
    public AssistenteTutoriaService(
            AvisoRepository avisoRepository,
            ServicoIA servicoIA,
            @Autowired(required = false) ServicoMenu servicoMenu) {
        this.avisoRepository = avisoRepository;
        this.servicoIA = servicoIA;
        this.servicoMenu = servicoMenu;
    }

    public AssistenteTutoriaService(AvisoRepository avisoRepository, ServicoIA servicoIA) {
        this(avisoRepository, servicoIA, null);
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

    public AssistenteChatResponseDTO responderCliente(Empresa empresa, AssistenteChatRequestDTO requisicao) {
        String modo = (requisicao.modo() == null || requisicao.modo().isBlank())
                ? "SUPORTE"
                : requisicao.modo().toUpperCase().trim();

        if ("TESTE_VIRTUAL".equals(modo) || "ATENDIMENTO_SANDBOX".equals(modo)) {
            return responderTesteVirtual(empresa, requisicao.mensagem());
        }

        return responderSuporteCliente(empresa, requisicao.mensagem());
    }

    private AssistenteChatResponseDTO responderTesteVirtual(Empresa empresa, String mensagem) {
        String prompt = construirPromptTesteVirtual(empresa, mensagem);
        String respostaIA = servicoIA != null ? servicoIA.gerarRespostaHumanizada(prompt) : null;

        String respostaFinal;
        if (respostaIA != null && !respostaIA.isBlank()) {
            respostaFinal = respostaIA.trim();
        } else {
            respostaFinal = gerarFallbackTesteVirtual(empresa, mensagem);
        }

        List<String> sugestoes = List.of(
                "Quais são os horários e valores?",
                "Menu de opções",
                "Onde fica localizado o espaço?",
                "Pode me enviar fotos do local?"
        );
        return new AssistenteChatResponseDTO(respostaFinal, "TESTE_VIRTUAL", Instant.now(), sugestoes);
    }

    private AssistenteChatResponseDTO responderSuporteCliente(Empresa empresa, String mensagem) {
        String base = obterBaseConhecimento();
        List<Aviso> avisos = avisoRepository != null
                ? avisoRepository.findByAtivoTrueOrderByIdDesc()
                : List.of();

        String prompt = construirPromptSuporteCliente(empresa, mensagem, base, avisos);
        String respostaIA = servicoIA != null ? servicoIA.gerarRespostaHumanizada(prompt) : null;

        String respostaFinal;
        if (respostaIA != null && !respostaIA.isBlank()) {
            respostaFinal = respostaIA.trim();
        } else {
            respostaFinal = gerarFallbackSuporteCliente(empresa, mensagem, avisos);
        }

        List<String> sugestoes = List.of(
                "Como conectar o WhatsApp via QR Code?",
                "Como sincronizar minha Google Agenda?",
                "Como cadastrar fotos e regras de locação?",
                "Onde vejo os tutoriais passo a passo?"
        );
        return new AssistenteChatResponseDTO(respostaFinal, "SUPORTE", Instant.now(), sugestoes);
    }

    private String construirPromptTesteVirtual(Empresa empresa, String mensagem) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é o Bot Virtual de Atendimento no WhatsApp para o estabelecimento:\n");
        if (empresa != null) {
            sb.append("Nome: ").append(empresa.getNome()).append("\n");
            sb.append("Ramo: ").append(empresa.getRamoDeAtuacao()).append("\n");
            sb.append("Mensagem de Saudação: ").append(empresa.getMensagemSaudacao()).append("\n");
            sb.append("Tabela de Preços: ").append(empresa.getTabelaDePrecos()).append("\n");
            sb.append("Regras de Locação: ").append(empresa.getRegrasLocacao()).append("\n");
            sb.append("Link Localização: ").append(empresa.getLinkGoogleMaps()).append("\n");
            if (servicoMenu != null) {
                sb.append("Menu Principal:\n").append(servicoMenu.montarMenuPrincipal(empresa)).append("\n");
            }
        }
        sb.append("Instrução: Responda exatamente como o bot responderia para o cliente final no WhatsApp. Seja direto, educado e use emojis moderados.\n");
        sb.append("Mensagem do Cliente: \"").append(mensagem.replace("\"", "'")).append("\"");
        return sb.toString();
    }

    private String gerarFallbackTesteVirtual(Empresa empresa, String mensagem) {
        String msg = mensagem.trim().toLowerCase();
        if (empresa == null) {
            return "Olá! Sou a assistente virtual de teste da sua empresa. Como posso ajudar?";
        }
        if (msg.equals("0") || msg.contains("menu") || msg.contains("oi") || msg.contains("olá") || msg.contains("ola") || msg.contains("iniciar")) {
            if (servicoMenu != null) {
                return servicoMenu.montarMenuPrincipal(empresa);
            }
            return (empresa.getMensagemSaudacao() != null && !empresa.getMensagemSaudacao().isBlank())
                    ? empresa.getMensagemSaudacao()
                    : "Olá! Bem-vindo(a) à " + empresa.getNome() + ". Como podemos te ajudar hoje?";
        }
        if (msg.contains("preço") || msg.contains("preco") || msg.contains("valor") || msg.contains("tabela") || msg.contains("quanto custa")) {
            return (empresa.getTabelaDePrecos() != null && !empresa.getTabelaDePrecos().isBlank())
                    ? empresa.getTabelaDePrecos()
                    : "Valores e disponibilidade de horários sob consulta.";
        }
        if (msg.contains("regra") || msg.contains("horario") || msg.contains("horário") || msg.contains("condiç") || msg.contains("condic")) {
            return (empresa.getRegrasLocacao() != null && !empresa.getRegrasLocacao().isBlank())
                    ? empresa.getRegrasLocacao()
                    : "Regras de atendimento e locação do estabelecimento.";
        }
        if (msg.contains("onde") || msg.contains("local") || msg.contains("endereço") || msg.contains("endereco") || msg.contains("mapa")) {
            return (empresa.getLinkGoogleMaps() != null && !empresa.getLinkGoogleMaps().isBlank())
                    ? "Nosso endereço e rota de acesso: " + empresa.getLinkGoogleMaps()
                    : "Consulte nosso endereço e localização no painel.";
        }
        if (msg.contains("foto") || msg.contains("galeria") || msg.contains("imagem")) {
            if (empresa.getLinkFotoPrincipal() != null && !empresa.getLinkFotoPrincipal().isBlank()) {
                return "Confira as fotos do nosso espaço: " + empresa.getLinkFotoPrincipal();
            }
            if (empresa.getLinkGaleria() != null && !empresa.getLinkGaleria().isBlank()) {
                return "Confira a galeria de fotos: " + empresa.getLinkGaleria();
            }
            return "Fotos e imagens disponíveis em nosso perfil.";
        }
        if (servicoMenu != null && msg.matches("^\\d+$")) {
            String acao = servicoMenu.descobrirAcao(empresa, msg);
            if (!"OPCAO_INVALIDA".equals(acao)) {
                return "Você selecionou a opção [" + acao + "]. Simulador processou com sucesso!";
            }
        }
        return "Recebido: \"" + mensagem + "\". O assistente virtual da empresa " + empresa.getNome() + " está operacional.";
    }

    private String construirPromptSuporteCliente(Empresa empresa, String mensagem, String base, List<Aviso> avisos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é o Assistente de Suporte e Tutoria do Tech-SaaS para clientes/proprietários de empresas.\n");
        if (empresa != null) {
            sb.append("Empresa do Usuário: ").append(empresa.getNome()).append(" (Sessão: ").append(empresa.getSessaoWhatsapp()).append(")\n");
            sb.append("Ramo: ").append(empresa.getRamoDeAtuacao()).append("\n");
        }
        sb.append("Base de Conhecimento Oficial do Sistema:\n");
        sb.append(base).append("\n\n");
        if (!avisos.isEmpty()) {
            sb.append("Avisos e Comunicados Ativos:\n");
            for (Aviso a : avisos) {
                sb.append("- [").append(a.getTipo()).append("] ").append(a.getTitulo()).append(": ").append(a.getConteudo()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("Instrução: Ensine o proprietário a configurar seu estabelecimento, horários, QR Code ou Google Agenda com instruções passo a passo claras.\n");
        sb.append("Dúvida do Usuário: \"").append(mensagem.replace("\"", "'")).append("\"");
        return sb.toString();
    }

    private String gerarFallbackSuporteCliente(Empresa empresa, String mensagem, List<Aviso> avisos) {
        String msgLower = mensagem.toLowerCase();
        if (msgLower.contains("whatsapp") || msgLower.contains("qr") || msgLower.contains("conectar")) {
            return "Para conectar seu WhatsApp: acesse a aba 'Conexão WhatsApp' no seu perfil, clique em 'Iniciar Conexão' e aponte a câmera do seu celular para o QR Code em 'Aparelhos Conectados'.";
        }
        if (msgLower.contains("agenda") || msgLower.contains("calendario") || msgLower.contains("calendário") || msgLower.contains("sincronizar")) {
            return "A sincronização com o Google Agenda integra suas reservas automaticamente. Verifique se o ID do calendário está preenchido no perfil da empresa.";
        }
        if (msgLower.contains("aviso") || msgLower.contains("tutorial") || msgLower.contains("tutoriais")) {
            if (!avisos.isEmpty()) {
                Aviso a = avisos.get(0);
                return "Consulte a tela de Tutoriais no menu. Aviso recente: [" + a.getTitulo() + "] " + a.getConteudo();
            }
            return "Acesse a seção de Tutoriais no menu lateral para visualizar guias passo a passo de configuração e operação.";
        }
        String nome = empresa != null ? empresa.getNome() : "sua empresa";
        return "Olá! Sou seu assistente de suporte para " + nome + ". Acesse os Tutoriais para configurar horários, agenda e WhatsApp ou alterne para a Sessão Virtual para testar seu bot.";
    }
}
