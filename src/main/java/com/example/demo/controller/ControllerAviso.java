package com.example.demo.controller;

import com.example.demo.dto.aviso.AvisoDTO;
import com.example.demo.model.Aviso;
import com.example.demo.repository.AvisoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@Tag(name = "Avisos", description = "Comunicados, tutoriais, manutenções e tabelas de preços do painel")
public class ControllerAviso {

    @Autowired
    private AvisoRepository avisoRepository;

    @Value("${admin.api.key}")
    private String adminApiKey;

    private boolean isAcessoNegado(String token) {
        return token == null || !token.equals(adminApiKey);
    }

    @GetMapping("/avisos")
    @Operation(summary = "Listar avisos ativos",
            description = "Devolve os avisos com ativo=true para o painel do cliente. Rota pública (sem x-admin-token).")
    @ApiResponse(responseCode = "200", description = "Lista de avisos ativos")
    public ResponseEntity<List<AvisoDTO>> listarAtivos() {
        List<AvisoDTO> avisos = avisoRepository.findByAtivoTrueOrderByIdDesc().stream()
                .map(this::paraDto)
                .toList();
        return ResponseEntity.ok(avisos);
    }

    @GetMapping("/admin/avisos")
    @Operation(summary = "Listar todos os avisos (admin)",
            description = "Devolve avisos ativos e inativos para a gestão no painel master. Exige x-admin-token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista completa de avisos"),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido")
    })
    public ResponseEntity<List<AvisoDTO>> listarTodos(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token) {

        exigirAdmin(token);
        List<AvisoDTO> avisos = avisoRepository.findAllByOrderByIdDesc().stream()
                .map(this::paraDto)
                .toList();
        return ResponseEntity.ok(avisos);
    }

    @PostMapping("/admin/avisos")
    @Operation(summary = "Criar aviso", description = "Cadastra um aviso a partir de AvisoDTO. Exige x-admin-token.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aviso criado",
                    content = @Content(schema = @Schema(implementation = AvisoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido")
    })
    public ResponseEntity<AvisoDTO> criar(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @Valid @RequestBody AvisoDTO dto) {

        exigirAdmin(token);
        Aviso aviso = new Aviso();
        aplicar(aviso, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paraDto(avisoRepository.save(aviso)));
    }

    @PutMapping("/admin/avisos/{id}")
    @Operation(summary = "Atualizar aviso",
            description = "Substitui título, tipo, conteúdo, link e flag ativo. Exige x-admin-token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aviso atualizado",
                    content = @Content(schema = @Schema(implementation = AvisoDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido"),
            @ApiResponse(responseCode = "404", description = "Aviso não encontrado")
    })
    public ResponseEntity<AvisoDTO> atualizar(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody AvisoDTO dto) {

        exigirAdmin(token);
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso não encontrado"));
        aplicar(aviso, dto);
        return ResponseEntity.ok(paraDto(avisoRepository.save(aviso)));
    }

    @DeleteMapping("/admin/avisos/{id}")
    @Operation(summary = "Excluir aviso", description = "Remove o aviso do banco. Exige x-admin-token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Aviso excluído"),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido"),
            @ApiResponse(responseCode = "404", description = "Aviso não encontrado")
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @PathVariable Long id) {

        exigirAdmin(token);
        if (!avisoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso não encontrado");
        }
        avisoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void exigirAdmin(String token) {
        if (isAcessoNegado(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido");
        }
    }

    private void aplicar(Aviso aviso, AvisoDTO dto) {
        aviso.setTipo(dto.tipo());
        aviso.setTitulo(dto.titulo());
        aviso.setConteudo(dto.conteudo());
        aviso.setLink(dto.link() == null || dto.link().isBlank() ? null : dto.link().trim());
        aviso.setAtivo(dto.ativo() == null || dto.ativo());
    }

    private AvisoDTO paraDto(Aviso aviso) {
        return new AvisoDTO(
                aviso.getId(),
                aviso.getTipo(),
                aviso.getTitulo(),
                aviso.getConteudo(),
                aviso.getLink(),
                aviso.getAtivo()
        );
    }
}
