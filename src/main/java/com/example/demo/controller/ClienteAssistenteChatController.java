package com.example.demo.controller;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.Usuario;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/assistente")
@Tag(name = "Assistente Cliente", description = "Assistente virtual e laboratório de testes do cliente")
public class ClienteAssistenteChatController {

    private final AssistenteTutoriaService assistenteTutoriaService;

    public ClienteAssistenteChatController(AssistenteTutoriaService assistenteTutoriaService) {
        this.assistenteTutoriaService = assistenteTutoriaService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Interagir com o assistente IA e sessão virtual de testes (Cliente)",
            description = "Canal de suporte para o cliente autenticado tirar dúvidas da sua empresa ou testar módulos em sessão virtual sem exigir QR Code físico.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta processada pela IA com sucesso",
                    content = @Content(schema = @Schema(implementation = AssistenteChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    public ResponseEntity<AssistenteChatResponseDTO> conversar(
            @Valid @RequestBody AssistenteChatRequestDTO requisicao) {
        if (requisicao == null || requisicao.mensagem() == null || requisicao.mensagem().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem obrigatória");
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated() || "anonymousUser".equals(autenticacao.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }

        Empresa empresa = null;
        if (autenticacao.getPrincipal() instanceof Usuario usuario) {
            empresa = usuario.getEmpresa();
        }

        AssistenteChatResponseDTO resposta = assistenteTutoriaService.responderCliente(empresa, requisicao);
        return ResponseEntity.ok(resposta);
    }
}
