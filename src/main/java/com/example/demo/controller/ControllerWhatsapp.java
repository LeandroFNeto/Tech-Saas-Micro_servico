package com.example.demo.controller;

import com.example.demo.dto.WhatsappWebhookDTO;
import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.EstadoUsuario;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.GerenciadorSessao;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControllerWhatsapp {

    @Autowired private GerenciadorSessao gerenciadorSessao;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private FactoryModulo moduloFactory;
    @Autowired private Observadorwhatsapp observador;
    @Autowired private ServicoWppConnect servicoWppConnect;
    @Value("${admin.api.key}") private String adminApiKey;

    @PostMapping("/webhook/whatsapp")
    @Tag(name = "Webhook WhatsApp", description = "Recepção tipada de eventos do WPPConnect")
    @Operation(
            summary = "Receber mensagem do WhatsApp",
            description = "Consome WhatsappWebhookDTO (sem Map genérico). Aplica travas Anti-X9, ignora grupos/status e encaminha o texto ao módulo da empresa."
    )
    @ApiResponse(responseCode = "200", description = "Evento aceito. Mensagens filtradas também retornam 200 para o WPPConnect não reenviar.",
            content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<Void> receberMensagem(@RequestBody WhatsappWebhookDTO payload) {
        System.out.println(payload.resumoRaioX());

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

            if (payload.enviadaPorMim()) {
                observador.logFiltro(sessao, rawFrom, "Dono do celular digitou (Anti-X9)");
                return ResponseEntity.ok().build();
            }

            if (payload.deveDescartarComoRuido()) {
                observador.logFiltro(sessao, rawFrom, "Mensagem ignorada (Grupo, Status ou Tipo Inválido)");
                return ResponseEntity.ok().build();
            }

            String chaveEstado = WhatsappUtil.normalizarParaChaveEstado(rawFrom);
            Empresa empresa = empresaRepository.buscarPorSessaoComModulos(sessao);

            if (empresa == null) {
                observador.logFiltro(sessao, rawFrom, "Empresa não encontrada para esta sessão");
                return ResponseEntity.ok().build();
            }

            EstadoUsuario estadoAtual = gerenciadorSessao.getEstado(chaveEstado);
            if (estadoAtual == null) {
                estadoAtual = EstadoUsuario.INICIO;
            }

            ModuloAtendimentoStrategy estrategia = moduloFactory.obterEstrategia(empresa.getRamoDeAtuacao());
            estrategia.processarMensagem(empresa, rawFrom, textoRecebido, estadoAtual.name());

            observador.logSucesso(sessao, rawFrom, textoRecebido);

        } catch (Exception e) {
            observador.logErro(sessao, rawFrom, "ControllerWhatsapp", e);
        }
        return ResponseEntity.ok().build();
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
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class)))
    })
    public ResponseEntity<SessaoStatusResponseDTO> consultarStatusSessao(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Token do painel do cliente")
            @RequestHeader(value = "x-cliente-token", required = false) String clienteToken,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao) {

        ResponseEntity<SessaoStatusResponseDTO> recusa = recusarSeNecessario(adminToken, clienteToken, sessao);
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
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada",
                    content = @Content(schema = @Schema(implementation = SessaoStatusResponseDTO.class)))
    })
    public ResponseEntity<SessaoStatusResponseDTO> iniciarSessaoWhatsapp(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Token do painel do cliente")
            @RequestHeader(value = "x-cliente-token", required = false) String clienteToken,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao) {

        ResponseEntity<SessaoStatusResponseDTO> recusa = recusarSeNecessario(adminToken, clienteToken, sessao);
        if (recusa != null) {
            return recusa;
        }
        return ResponseEntity.ok(servicoWppConnect.iniciarSessao(sessao));
    }

    private ResponseEntity<SessaoStatusResponseDTO> recusarSeNecessario(
            String adminToken, String clienteToken, String sessao) {
        boolean adminOk = possuiPapel("ROLE_ADMIN")
                || (adminToken != null && adminToken.equals(adminApiKey));
        boolean clienteOk = possuiPapel("ROLE_CLIENTE")
                || (clienteToken != null && !clienteToken.isBlank());
        if (!adminOk && !clienteOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null));
        }
        if (empresaRepository.findBySessaoWhatsapp(sessao) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SessaoStatusResponseDTO(sessao, "DISCONNECTED", null));
        }
        return null;
    }

    private boolean possuiPapel(String papel) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(papel::equals);
    }
}
