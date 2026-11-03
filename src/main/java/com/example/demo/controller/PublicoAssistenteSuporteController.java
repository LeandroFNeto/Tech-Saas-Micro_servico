package com.example.demo.controller;

import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.dto.assistente.SuportePublicoRequestDTO;
import com.example.demo.servico.LimitadorPorIp;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/publico/assistente")
@Tag(name = "Assistente Público", description = "Bot de Suporte sem login, usado no login e na apresentação")
public class PublicoAssistenteSuporteController {

    private final AssistenteTutoriaService assistenteTutoriaService;
    private final LimitadorPorIp limitador;

    public PublicoAssistenteSuporteController(AssistenteTutoriaService assistenteTutoriaService, LimitadorPorIp limitador) {
        this.assistenteTutoriaService = assistenteTutoriaService;
        this.limitador = limitador;
    }

    @PostMapping("/suporte")
    @Operation(summary = "Tirar dúvida sobre o app com o bot de Suporte, sem login",
            description = "Responde só com a base de conhecimento e os avisos ativos, sem dado de empresa. Limite de 10 mensagens por minuto por IP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta do bot de Suporte",
                    content = @Content(schema = @Schema(implementation = AssistenteChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Mensagem vazia ou acima de 500 caracteres"),
            @ApiResponse(responseCode = "429", description = "Limite de mensagens por minuto excedido para o IP")
    })
    public ResponseEntity<AssistenteChatResponseDTO> conversar(
            @Valid @RequestBody SuportePublicoRequestDTO requisicao, HttpServletRequest http) {
        if (!limitador.permitir(http.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de mensagens excedido");
        }
        return ResponseEntity.ok(assistenteTutoriaService.responderSuporte(requisicao.mensagem()));
    }
}
