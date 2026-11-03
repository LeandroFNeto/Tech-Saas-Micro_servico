package com.example.demo.controller;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.dto.assistente.DiagnosticoResponseDTO;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import com.example.demo.servico.assistente.DiagnosticoModulosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/assistente-tutoria")
@Tag(name = "Assistente Tutoria", description = "Módulo administrativo interno de diagnóstico de integridade e assistência com IA")
public class AdminAssistenteTutoriaController {

    private final DiagnosticoModulosService diagnosticoModulosService;
    private final AssistenteTutoriaService assistenteTutoriaService;

    @Value("${admin.api.key}")
    private String adminApiKey;

    public AdminAssistenteTutoriaController(
            DiagnosticoModulosService diagnosticoModulosService,
            AssistenteTutoriaService assistenteTutoriaService) {
        this.diagnosticoModulosService = diagnosticoModulosService;
        this.assistenteTutoriaService = assistenteTutoriaService;
    }

    private boolean isAcessoNegado(String token) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals)) {
            return false;
        }
        return token == null || !token.equals(adminApiKey);
    }

    @GetMapping("/diagnostico")
    @Operation(summary = "Diagnosticar integridade de todos os módulos (Admin)",
            description = "Executa checagem de saúde não destrutiva em lote (Auth, Avisos, Agenda, Empresa, WhatsApp, IA).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório de diagnóstico dos módulos gerado com sucesso",
                    content = @Content(schema = @Schema(implementation = DiagnosticoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    public ResponseEntity<DiagnosticoResponseDTO> diagnosticar(
            @Parameter(description = "Token administrativo")
            @RequestHeader(value = "x-admin-token", required = false) String token) {
        if (isAcessoNegado(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DiagnosticoResponseDTO resposta = diagnosticoModulosService.executarDiagnostico();
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/chat")
    @Operation(summary = "Interagir com o assistente IA de tutoria e diagnóstico (Admin)",
            description = "Recebe dúvidas operacionais, pedidos de ajuda de configuração ou comandos de teste.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta processada pela IA com sucesso",
                    content = @Content(schema = @Schema(implementation = AssistenteChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    public ResponseEntity<AssistenteChatResponseDTO> conversar(
            @Parameter(description = "Token administrativo")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @Valid @RequestBody AssistenteChatRequestDTO requisicao) {
        if (isAcessoNegado(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (requisicao == null || requisicao.mensagem() == null || requisicao.mensagem().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AssistenteChatResponseDTO resposta = assistenteTutoriaService.responder(requisicao);
        return ResponseEntity.ok(resposta);
    }
}
