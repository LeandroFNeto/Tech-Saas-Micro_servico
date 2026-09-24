package com.example.demo.controller;

import com.example.demo.dto.empresa.ColecaoEmpresaHalDTO;
import com.example.demo.dto.empresa.EmpresaCreateDTO;
import com.example.demo.dto.empresa.EmpresaPainelDTO;
import com.example.demo.dto.empresa.EmpresaResponseDTO;
import com.example.demo.dto.empresa.EmpresaUpdateAdminDTO;
import com.example.demo.dto.empresa.EmpresaUpdateDTO;
import com.example.demo.dto.empresa.ModuloMenuDTO;
import com.example.demo.dto.empresa.ResetSenhaAdminDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.ModuloEmpresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.CatalogoModulos;
import com.example.demo.servico.ServicoAuth;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/empresas")
@Tag(name = "Gerenciamento de Empresas", description = "Cadastro e atualização de empresas do SaaS, isolados por intenção (criação, cliente e admin)")
public class ControllerEmpresa {

    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ServicoAuth servicoAuth;
    @Value("${admin.api.key}") private String adminApiKey;

    private boolean possuiPapel(String papel) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(papel::equals);
    }

    private boolean isAcessoNegado(String token) {
        if (possuiPapel("ROLE_ADMIN")) {
            return false;
        }
        return token == null || !token.equals(adminApiKey);
    }

    private boolean isClienteSemToken(String token) {
        if (possuiPapel("ROLE_CLIENTE") || possuiPapel("ROLE_ADMIN")) {
            return false;
        }
        return token == null || token.isBlank();
    }

    @GetMapping
    @Operation(summary = "Listar empresas cadastradas",
            description = "Devolve a coleção HAL (_embedded.empresas) para o painel admin. Exige x-admin-token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista HAL de empresas"),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido")
    })
    public ResponseEntity<ColecaoEmpresaHalDTO> listar(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token) {

        if (isAcessoNegado(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido");
        }

        List<EmpresaPainelDTO> itens = empresaRepository.listarComModulos().stream()
                .map(this::paraPainel)
                .toList();
        return ResponseEntity.ok(new ColecaoEmpresaHalDTO(new ColecaoEmpresaHalDTO.Embedded(itens)));
    }

    @GetMapping("/search/findBySessaoWhatsapp")
    @Operation(summary = "Buscar empresa pela sessão WhatsApp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "401", description = "Token administrativo ou de cliente inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada")
    })
    public ResponseEntity<EmpresaPainelDTO> buscarPorSessao(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Token do painel do cliente")
            @RequestHeader(value = "x-cliente-token", required = false) String clienteToken,
            @RequestParam String sessaoWhatsapp) {

        if (isAcessoNegado(adminToken) && isClienteSemToken(clienteToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido");
        }

        Empresa empresa = empresaRepository.buscarPorSessaoComModulos(sessaoWhatsapp);
        if (empresa == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(paraPainel(empresa));
    }

    @PostMapping
    @Operation(summary = "Criar novo cliente SaaS", description = "Cadastra uma empresa a partir de EmpresaCreateDTO e devolve EmpresaResponseDTO, sem expor a entidade JPA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empresa criada com sucesso",
                    content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Sessão WhatsApp já ocupada"),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido")
    })
    public ResponseEntity<EmpresaResponseDTO> cadastrar(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @Valid @RequestBody EmpresaCreateDTO dto) {

        if (isAcessoNegado(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido");
        }

        if (empresaRepository.findBySessaoWhatsapp(dto.sessaoWhatsapp()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sessão já ocupada");
        }

        String emailAcesso = ServicoAuth.emailAcessoDaSessao(dto.sessaoWhatsapp());
        servicoAuth.garantirEmailDisponivel(emailAcesso);

        Empresa empresa = new Empresa();
        empresa.setNome(dto.nome());
        empresa.setSessaoWhatsapp(dto.sessaoWhatsapp());
        empresa.setRamoDeAtuacao(dto.ramoDeAtuacao());
        if (dto.googleCalendarId() != null && !dto.googleCalendarId().isBlank()) {
            empresa.setGoogleCalendarId(dto.googleCalendarId().trim());
        }
        if (dto.precoBase() != null) {
            empresa.setTabelaDePrecos(dto.precoBase().toString());
        }
        if (dto.permiteReservaAutomatica() != null) {
            empresa.setPermiteReservaAutomatica(dto.permiteReservaAutomatica());
        }
        if (dto.linkFotoPrincipal() != null) {
            empresa.setLinkFotoPrincipal(dto.linkFotoPrincipal());
        }
        if (dto.urlsGaleria() != null) {
            empresa.setUrlsGaleria(dto.urlsGaleria());
        }
        if (dto.linkGaleria() != null) {
            empresa.setLinkGaleria(dto.linkGaleria().isBlank() ? null : dto.linkGaleria().trim());
        }
        CatalogoModulos.aplicar(empresa, dto.modulosIniciais());

        Empresa salva = empresaRepository.save(empresa);
        servicoAuth.criarUsuarioCliente(salva, emailAcesso, dto.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(salva));
    }

    @PutMapping("/{sessao}/cliente")
    @Operation(summary = "Atualizar perfil pelo cliente", description = "Aplica EmpresaUpdateDTO: textos, links e aparência. Não altera cobrança nem infraestrutura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil do cliente atualizado",
                    content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada")
    })
    public ResponseEntity<EmpresaResponseDTO> atualizarPeloCliente(
            @Parameter(description = "Token do painel do cliente")
            @RequestHeader(value = "x-cliente-token", required = false) String token,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao,
            @Valid @RequestBody EmpresaUpdateDTO dto) {

        Empresa empresa = empresaRepository.buscarPorSessaoComModulos(sessao);
        if (empresa == null) {
            return ResponseEntity.notFound().build();
        }

        if (dto.nome() != null) empresa.setNome(dto.nome());
        if (dto.mensagemSaudacao() != null) empresa.setMensagemSaudacao(dto.mensagemSaudacao());
        if (dto.tabelaDePrecos() != null) empresa.setTabelaDePrecos(dto.tabelaDePrecos());
        if (dto.linkGoogleMaps() != null) empresa.setLinkGoogleMaps(dto.linkGoogleMaps());
        if (dto.linkFotoPrincipal() != null) empresa.setLinkFotoPrincipal(dto.linkFotoPrincipal());
        if (dto.urlsGaleria() != null) empresa.setUrlsGaleria(dto.urlsGaleria());
        if (dto.linkGaleria() != null) {
            empresa.setLinkGaleria(dto.linkGaleria().isBlank() ? null : dto.linkGaleria().trim());
        }
        if (dto.permiteReservaAutomatica() != null) {
            empresa.setPermiteReservaAutomatica(dto.permiteReservaAutomatica());
        }
        if (dto.locacaoPorHora() != null) {
            empresa.setLocacaoPorHora(dto.locacaoPorHora());
        }
        if (dto.regrasLocacao() != null) {
            empresa.setRegrasLocacao(dto.regrasLocacao());
        }
        if (dto.modulosMenu() != null) {
            for (var item : dto.modulosMenu()) {
                CatalogoModulos.upsertItemMenu(empresa, item.codigoAcao(), item.textoMenu(), item.ativo());
            }
        }

        return ResponseEntity.ok(paraResposta(empresaRepository.save(empresa)));
    }

    @PutMapping("/{sessao}/admin")
    @Operation(summary = "Atualizar infraestrutura pelo admin", description = "Aplica EmpresaUpdateAdminDTO: sessão, tipo de locação, ID da agenda Google e módulos. Uso restrito ao administrador do SaaS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Infraestrutura atualizada",
                    content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada")
    })
    public ResponseEntity<EmpresaResponseDTO> atualizarPeloAdmin(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao,
            @Valid @RequestBody EmpresaUpdateAdminDTO dto) {

        if (isAcessoNegado(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Acesso negado");
        }

        Empresa empresa = empresaRepository.buscarPorSessaoComModulos(sessao);
        if (empresa == null) {
            return ResponseEntity.notFound().build();
        }

        if (dto.sessaoWhatsapp() != null) empresa.setSessaoWhatsapp(dto.sessaoWhatsapp());
        if (dto.locacaoPorHora() != null) empresa.setLocacaoPorHora(dto.locacaoPorHora());
        if (dto.googleCalendarId() != null) empresa.setGoogleCalendarId(dto.googleCalendarId().isBlank() ? null : dto.googleCalendarId().trim());
        if (dto.linkFotoPrincipal() != null) empresa.setLinkFotoPrincipal(dto.linkFotoPrincipal());
        if (dto.urlsGaleria() != null) empresa.setUrlsGaleria(dto.urlsGaleria());
        if (dto.linkGaleria() != null) {
            empresa.setLinkGaleria(dto.linkGaleria().isBlank() ? null : dto.linkGaleria().trim());
        }
        CatalogoModulos.aplicar(empresa, dto.modulosAtivos());

        return ResponseEntity.ok(paraResposta(empresaRepository.save(empresa)));
    }

    @PutMapping("/{sessao}/reset-senha")
    @Operation(summary = "Resetar senha de acesso do cliente",
            description = "O administrador sobrescreve a senha do usuário da empresa. Não exige a senha atual. Exige x-admin-token.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha atualizada"),
            @ApiResponse(responseCode = "400", description = "Nova senha inválida"),
            @ApiResponse(responseCode = "401", description = "Token administrativo inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa ou usuário do cliente não encontrado")
    })
    public ResponseEntity<Void> resetarSenha(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String token,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao,
            @Valid @RequestBody ResetSenhaAdminDTO dto) {

        if (isAcessoNegado(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Acesso negado");
        }

        if (empresaRepository.findBySessaoWhatsapp(sessao) == null) {
            return ResponseEntity.notFound().build();
        }

        servicoAuth.resetarSenhaDoCliente(sessao, dto.novaSenha());
        return ResponseEntity.noContent().build();
    }

    private EmpresaResponseDTO paraResposta(Empresa empresa) {
        List<ModuloMenuDTO> menu = (empresa.getModulosAtivos() == null
                ? List.<ModuloEmpresa>of()
                : empresa.getModulosAtivos())
                .stream()
                .filter(modulo -> CatalogoModulos.isCodigoMenuBot(modulo.getCodigoAcao()))
                .map(modulo -> new ModuloMenuDTO(
                        modulo.getCodigoAcao(),
                        modulo.getTextoMenu(),
                        modulo.getAtivo(),
                        modulo.getOrdemExibicao()))
                .toList();

        return new EmpresaResponseDTO(
                empresa.getId(),
                empresa.getNome(),
                empresa.getSessaoWhatsapp(),
                "DISCONNECTED",
                empresa.getLinkGoogleMaps(),
                empresa.getLinkFotoPrincipal(),
                List.copyOf(empresa.getUrlsGaleria()),
                empresa.getLinkGaleria(),
                Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()),
                Boolean.TRUE.equals(empresa.getLocacaoPorHora()),
                empresa.getRegrasLocacao(),
                menu,
                LocalDateTime.now()
        );
    }

    private EmpresaPainelDTO paraPainel(Empresa empresa) {
        List<EmpresaPainelDTO.ModuloPainelDTO> modulos = (empresa.getModulosAtivos() == null
                ? List.<ModuloEmpresa>of()
                : empresa.getModulosAtivos())
                .stream()
                .map(modulo -> new EmpresaPainelDTO.ModuloPainelDTO(
                        modulo.getCodigoAcao(),
                        modulo.getTextoMenu(),
                        modulo.getOrdemExibicao(),
                        modulo.getAtivo()))
                .toList();

        return new EmpresaPainelDTO(
                empresa.getId(),
                empresa.getNome(),
                empresa.getUsaIA(),
                empresa.getSessaoWhatsapp(),
                "DISCONNECTED",
                empresa.getMensagemSaudacao(),
                empresa.getRamoDeAtuacao(),
                empresa.getTabelaDePrecos(),
                empresa.getRegrasLocacao(),
                empresa.getLinkGoogleMaps(),
                empresa.getLinkFotoPrincipal(),
                List.copyOf(empresa.getUrlsGaleria()),
                empresa.getLinkGaleria(),
                empresa.getGoogleCalendarId(),
                empresa.getLocacaoPorHora(),
                Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()),
                modulos,
                LocalDateTime.now()
        );
    }
}
