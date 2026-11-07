package com.example.demo.controller;

import com.example.demo.dto.WhatsappWebhookDTO;
import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.security.WebhookSecretFilter;
import com.example.demo.servico.GerenciadorSessao;
import com.example.demo.servico.RegistroEnviosBot;
import com.example.demo.servico.ServicoAcessoEmpresa;
import com.example.demo.servico.ServicoContato;
import com.example.demo.servico.ServicoRespostaLembrete;
import com.example.demo.servico.ServicoWppConnect;
import com.example.demo.servico.observador.Observadorwhatsapp;
import com.example.demo.strategy.FactoryModulo;
import com.example.demo.strategy.ModuloAtendimentoStrategy;
import com.example.demo.util.WhatsappUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;

@RestController
public class ControllerWhatsapp {

    private static final Logger log = LoggerFactory.getLogger(ControllerWhatsapp.class);

    @Autowired private GerenciadorSessao gerenciadorSessao;
    @Autowired private RegistroEnviosBot registroEnviosBot;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private FactoryModulo moduloFactory;
    @Autowired private Observadorwhatsapp observador;
    @Autowired private ServicoWppConnect servicoWppConnect;
    @Autowired private ServicoAcessoEmpresa servicoAcessoEmpresa;
    @Autowired private ServicoContato servicoContato;
    @Autowired private ServicoRespostaLembrete servicoRespostaLembrete;
    @Value("${wppconnect.webhook-secret:}") private String webhookSecret;
    @Autowired(required = false) private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @PostMapping("/webhook/whatsapp")
    @Tag(name = "Webhook WhatsApp", description = "Recepção tipada de eventos do WPPConnect")
    @Operation(
            summary = "Receber mensagem do WhatsApp",
            description = "Consome WhatsappWebhookDTO (sem Map genérico). Aplica travas Anti-X9, ignora grupos/status e encaminha o texto ao módulo da empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento aceito. Mensagens filtradas também retornam 200 para o WPPConnect não reenviar.",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Cabeçalho x-webhook-token ausente, inválido ou segredo não configurado no ambiente.",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Void> receberMensagem(
            @Parameter(description = "Segredo compartilhado enviado pelo WPPConnect")
            @RequestHeader(value = WebhookSecretFilter.HEADER_WEBHOOK_TOKEN, required = false) String webhookToken,
            @RequestBody WhatsappWebhookDTO payload) {
        if (!WebhookSecretFilter.segredoValido(webhookSecret, webhookToken)) {
            log.warn("Webhook WhatsApp recusado para sessão {}: cabeçalho {} ausente ou inválido",
                    payload != null ? payload.session() : "desconhecida",
                    WebhookSecretFilter.HEADER_WEBHOOK_TOKEN);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug(payload.resumoRaioX());

        String sessao = payload.session();
        String rawFrom = payload.resolverRemoteJid();
        String textoRecebido = payload.resolverTexto();

        try {
            if (payload.ehEventoQrcode()) {
                servicoWppConnect.guardarQrcode(sessao, payload.qrcode());
                observador.logFiltro(sessao, null, "QR Code recebido do WPPConnect");
                return ResponseEntity.ok().build();
            }

            if (payload.ehEventoConexao()) {
                return ResponseEntity.ok().build();
            }

            if (payload.ehMensagemAntiga(clock)) {
                observador.logFiltro(sessao, rawFrom, "Mensagem antiga descartada");
                return ResponseEntity.ok().build();
            }

            if (payload.enviadaPorMim()) {
                String contatoAlvo = payload.resolverContatoAlvo();
                if (foiEnviadoPeloBot(sessao, contatoAlvo, textoRecebido)) {
                    observador.logFiltro(sessao, rawFrom, "Eco do bot ignorado");
                    return ResponseEntity.ok().build();
                }
                if (contatoAlvo != null && !contatoAlvo.isBlank()) {
                    gerenciadorSessao.ativarTravaProprietario(sessao, WhatsappUtil.normalizarParaChaveEstado(contatoAlvo));
                }
                observador.logFiltro(sessao, rawFrom, "Dono do celular digitou (Anti-X9)");
                return ResponseEntity.ok().build();
            }

            if (payload.deveDescartarComoRuido()) {
                observador.logFiltro(sessao, rawFrom, "Mensagem ignorada (Grupo, Status ou Tipo Inválido)");
                return ResponseEntity.ok().build();
            }

            String jidBruto = payload.resolverJidBruto();
            if (jidBruto != null && !jidBruto.equals(rawFrom)) {
                gerenciadorSessao.registrarApelido(sessao, jidBruto, rawFrom);
            }
            String chaveEstado = WhatsappUtil.normalizarParaChaveEstado(rawFrom);

            if (gerenciadorSessao.estaComTravaProprietarioAtiva(sessao, chaveEstado)) {
                observador.logFiltro(sessao, rawFrom, "Atendimento pausado: proprietário atendeu nos últimos 20 minutos");
                return ResponseEntity.ok().build();
            }
            Empresa empresa = empresaRepository.buscarPorSessaoComModulos(sessao);

            if (empresa == null || Boolean.FALSE.equals(empresa.getAtivo())) {
                observador.logFiltro(sessao, rawFrom, "Empresa não encontrada ou inativa para esta sessão");
                return ResponseEntity.ok().build();
            }

            servicoContato.registrarOuAtualizarContato(empresa, rawFrom, payload.resolverNomeRemetente());

            if (servicoContato.processarOptOutPorMensagem(empresa, rawFrom, textoRecebido)) {
                observador.logSucesso(sessao, rawFrom, "Opt-out processado a pedido do cliente");
                return ResponseEntity.ok().build();
            }

            EstadoUsuario estadoAtual = gerenciadorSessao.getEstado(sessao, chaveEstado);
            if (estadoAtual == null) {
                estadoAtual = EstadoUsuario.INICIO;
            }

            boolean tratouLembrete = servicoRespostaLembrete.processarPossivelResposta(empresa, rawFrom, textoRecebido, estadoAtual);
            if (tratouLembrete) {
                observador.logSucesso(sessao, rawFrom, textoRecebido);
                return ResponseEntity.ok().build();
            }

            ModuloAtendimentoStrategy estrategia = moduloFactory.obterEstrategia(empresa.getRamoDeAtuacao());
            estrategia.processarMensagem(empresa, rawFrom, textoRecebido, estadoAtual.name());

            observador.logSucesso(sessao, rawFrom, textoRecebido);

        } catch (Exception e) {
            observador.logErro(sessao, rawFrom, "ControllerWhatsapp", e);
        }
        return ResponseEntity.ok().build();
    }

    private boolean foiEnviadoPeloBot(String sessao, String contatoAlvo, String texto) {
        if (registroEnviosBot.foiEnviadoPeloBot(sessao, contatoAlvo, texto)) {
            return true;
        }
        String apelido = gerenciadorSessao.apelidoDe(sessao, contatoAlvo);
        return apelido != null && registroEnviosBot.foiEnviadoPeloBot(sessao, apelido, texto);
    }

    @GetMapping({"/whatsapp/{sessao}/status", "/whatsapp/status/{sessao}"})
    @Tag(name = "Conexão WhatsApp", description = "BFF do painel para status e QR Code da sessão no WPPConnect")
    @Operation(summary = "Consultar status da sessão WhatsApp",
            description = "O Java consulta o WPPConnect (GET /api/{sessao}/status-session) e devolve SessaoStatusResponseDTO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atual da sessão",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token administrativo ou de cliente inválido",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Cliente tentando acessar sessão de outra empresa ou inexistente",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada (admin)",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class)))
    })
    public ResponseEntity<SessaoStatusResponseDTO> consultarStatusSessao(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao) {

        ResponseEntity<SessaoStatusResponseDTO> recusa = recusarSeNecessario(adminToken, sessao, false);
        if (recusa != null) {
            return recusa;
        }
        return ResponseEntity.ok(servicoWppConnect.consultarStatus(sessao));
    }

    @PostMapping({"/whatsapp/{sessao}/iniciar", "/whatsapp/iniciar/{sessao}"})
    @Tag(name = "Conexão WhatsApp")
    @Operation(summary = "Iniciar sessão WhatsApp e gerar QR Code",
            description = "Se a sessão já estiver conectada, devolve essa conexão. Caso contrário dispara POST /api/{sessao}/start-session no WPPConnect e reabre o token salvo após uma queda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão iniciada (QR Code pode aparecer em seguida)",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token administrativo ou de cliente inválido",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Cliente tentando acessar sessão de outra empresa ou inexistente",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada (admin)",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class)))
    })
    public ResponseEntity<SessaoStatusResponseDTO> iniciarSessaoWhatsapp(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao) {

        ResponseEntity<SessaoStatusResponseDTO> recusa = recusarSeNecessario(adminToken, sessao, true);
        if (recusa != null) {
            return recusa;
        }
        return ResponseEntity.ok(servicoWppConnect.iniciarSessao(sessao));
    }

    private ResponseEntity<SessaoStatusResponseDTO> recusarSeNecessario(String adminToken, String sessao, boolean exigirEscrita) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        try {
            if (exigirEscrita) {
                servicoAcessoEmpresa.exigirNaoLeitura(autenticacao);
            }
            servicoAcessoEmpresa.validarAcessoSessao(autenticacao, adminToken, sessao);
            return null;
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null));
        }
    }
}
