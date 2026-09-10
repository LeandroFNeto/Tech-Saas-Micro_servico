package com.example.demo.controller;

import com.example.demo.dto.auth.AlterarSenhaDTO;
import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.LoginResponseDTO;
import com.example.demo.model.Usuario;
import com.example.demo.servico.ServicoAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login JWT e troca de senha do painel")
public class ControllerAuth {

    private final ServicoAuth servicoAuth;

    public ControllerAuth(ServicoAuth servicoAuth) {
        this.servicoAuth = servicoAuth;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e emitir JWT",
            description = "Valida e-mail e senha (BCrypt) e devolve LoginResponseDTO. Rota pública.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos")
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(servicoAuth.login(dto));
    }

    @PutMapping("/alterar-senha")
    @Operation(summary = "Alterar senha do usuário autenticado",
            description = "Exige JWT no header Authorization Bearer. Aplica AlterarSenhaDTO e persiste o hash BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha atualizada"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou senha atual incorreta"),
            @ApiResponse(responseCode = "400", description = "Nova senha inválida")
    })
    public ResponseEntity<Void> alterarSenha(Authentication authentication, @Valid @RequestBody AlterarSenhaDTO dto) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autorizado");
        }
        servicoAuth.alterarSenha(usuario.getEmail(), dto);
        return ResponseEntity.noContent().build();
    }
}
