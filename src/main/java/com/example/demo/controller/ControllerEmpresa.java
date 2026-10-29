package com.example.demo.controller;

import com.example.demo.dto.empresa.ColecaoEmpresaHalDTO;
import com.example.demo.dto.empresa.EmpresaCreateDTO;
import com.example.demo.dto.empresa.EmpresaPainelDTO;
import com.example.demo.dto.empresa.EmpresaResponseDTO;
import com.example.demo.dto.empresa.EmpresaUpdateAdminDTO;
import com.example.demo.dto.empresa.EmpresaUpdateDTO;
import com.example.demo.dto.empresa.ModuloMenuDTO;
import com.example.demo.dto.empresa.ResetSenhaAdminDTO;
import com.example.demo.dto.previsao.PrevisaoTempoResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.ModuloEmpresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.CatalogoModulos;
import com.example.demo.servico.ServicoAcessoEmpresa;
import com.example.demo.servico.ServicoAuth;
import com.example.demo.servico.previsao.ProvedorIndisponivelException;
import com.example.demo.servico.previsao.ProvedorPrevisao;
import com.example.demo.servico.previsao.ServicoPrevisaoTempo;
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
    @Autowired private ServicoPrevisaoTempo servicoPrevisaoTempo;
    @Autowired private ServicoAcessoEmpresa servicoAcessoEmpresa;
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
                .map(empresa -> paraPainel(empresa, true))
                .toList();
        return ResponseEntity.ok(new ColecaoEmpresaHalDTO(new ColecaoEmpresaHalDTO.Embedded(itens)));
    }

    @GetMapping("/search/findBySessaoWhatsapp")
    @Operation(summary = "Buscar empresa pela sessão WhatsApp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "401", description = "Token administrativo ou de cliente inválido"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa ou sessão inexistente para quem não é admin"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada (admin)")
    })
    public ResponseEntity<EmpresaPainelDTO> buscarPorSessao(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @RequestParam String sessaoWhatsapp) {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoSessao(autenticacao, adminToken, sessaoWhatsapp);
        boolean admin = servicoAcessoEmpresa.isAdmin(autenticacao, adminToken);
        return ResponseEntity.ok(paraPainel(empresa, admin));
    }

    @PostMapping
    @Operation(summary = "Criar novo cliente SaaS", description = "Cadastra uma empresa a partir de EmpresaCreateDTO e devolve EmpresaResponseDTO, sem expor a entidade JPA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empresa criada com sucesso",
                    content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Sessão WhatsApp já ocupada ou reserva automática ligada sem agenda do Google"),
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
            exigirAgendaParaReservaAutomatica(dto.permiteReservaAutomatica(), empresa.getGoogleCalendarId());
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
            @ApiResponse(responseCode = "400", description = "Cidade não encontrada ou tentativa de alterar a reserva automática, liberada só pelo administrador"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa ou sessão inexistente para quem não é admin"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada (admin)"),
            @ApiResponse(responseCode = "429", description = "Limite de 5 trocas de cidade por dia atingido"),
            @ApiResponse(responseCode = "503", description = "Provedor de previsão indisponível")
    })
    public ResponseEntity<EmpresaResponseDTO> atualizarPeloCliente(
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao,
            @Valid @RequestBody EmpresaUpdateDTO dto) {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoSessao(autenticacao, null, sessao);

        if (dto.permiteReservaAutomatica() != null
                && !dto.permiteReservaAutomatica().equals(Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A reserva automática é liberada pelo administrador.");
        }

        ProvedorPrevisao.Localizacao novaLocalizacao = null;
        boolean limparCidade = false;
        String cidadeNova = null;
        if (dto.cidadePrevisao() != null) {
            cidadeNova = dto.cidadePrevisao().trim();
            if (cidadeNova.isEmpty()) {
                limparCidade = true;
            } else if (!cidadeNova.equalsIgnoreCase(empresa.getCidadePrevisao()) || empresa.getLatitude() == null) {
                novaLocalizacao = geocodificarCidade(empresa, cidadeNova);
            }
        }

        if (limparCidade) {
            empresa.setCidadePrevisao(null);
            empresa.setCidadePrevisaoResolvida(null);
            empresa.setLatitude(null);
            empresa.setLongitude(null);
        } else if (novaLocalizacao != null) {
            empresa.setCidadePrevisao(cidadeNova);
            empresa.setCidadePrevisaoResolvida(novaLocalizacao.nomeResolvido());
            empresa.setLatitude(novaLocalizacao.latitude());
            empresa.setLongitude(novaLocalizacao.longitude());
        }
        if (dto.previsaoTempoAtiva() != null) {
            empresa.setPrevisaoTempoAtiva(dto.previsaoTempoAtiva());
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

    @GetMapping("/{sessao}/previsao")
    @Operation(summary = "Previsão do tempo dos próximos dias da empresa",
            description = "Devolve PrevisaoTempoResponseDTO. Aceita o admin ou o cliente da própria empresa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Previsão, ou disponivel=false",
                    content = @Content(schema = @Schema(implementation = PrevisaoTempoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Sem token"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa ou sessão inexistente para quem não é admin"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para a sessão informada (admin)")
    })
    public ResponseEntity<PrevisaoTempoResponseDTO> buscarPrevisao(
            @Parameter(description = "Token administrativo do SaaS", example = "sua-chave-admin")
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao) {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoSessao(autenticacao, adminToken, sessao);
        return ResponseEntity.ok(servicoPrevisaoTempo.consultar(empresa));
    }

    @PutMapping("/{sessao}/admin")
    @Operation(summary = "Atualizar infraestrutura pelo admin", description = "Aplica EmpresaUpdateAdminDTO: sessão, tipo de locação, ID da agenda Google e módulos. Uso restrito ao administrador do SaaS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Infraestrutura atualizada",
                    content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Reserva automática passando de desligada para ligada sem agenda do Google"),
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

        if (Boolean.TRUE.equals(dto.permiteReservaAutomatica())
                && !Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica())) {
            String agendaFinal = dto.googleCalendarId() != null
                    ? (dto.googleCalendarId().isBlank() ? null : dto.googleCalendarId().trim())
                    : empresa.getGoogleCalendarId();
            exigirAgendaParaReservaAutomatica(true, agendaFinal);
        }

        if (dto.sessaoWhatsapp() != null) empresa.setSessaoWhatsapp(dto.sessaoWhatsapp());
        if (dto.locacaoPorHora() != null) empresa.setLocacaoPorHora(dto.locacaoPorHora());
        if (dto.permiteReservaAutomatica() != null) empresa.setPermiteReservaAutomatica(dto.permiteReservaAutomatica());
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

    private void exigirAgendaParaReservaAutomatica(boolean ligar, String googleCalendarId) {
        if (ligar && (googleCalendarId == null || googleCalendarId.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cadastre a agenda do Google antes de ligar a reserva automática.");
        }
    }

    private ProvedorPrevisao.Localizacao geocodificarCidade(Empresa empresa, String cidade) {
        if (!servicoPrevisaoTempo.consumirTrocaDeCidade(empresa.getId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas alterações de cidade hoje. Tente amanhã.");
        }
        try {
            return servicoPrevisaoTempo.geocodificar(cidade)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cidade não encontrada. Confira o nome da cidade."));
        } catch (ProvedorIndisponivelException falha) {
            servicoPrevisaoTempo.estornarTrocaDeCidade(empresa.getId());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível verificar a cidade agora. Tente novamente em alguns minutos.");
        }
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
                empresa.getCidadePrevisao(),
                empresa.getCidadePrevisaoResolvida(),
                Boolean.TRUE.equals(empresa.getPrevisaoTempoAtiva()),
                LocalDateTime.now()
        );
    }

    private EmpresaPainelDTO paraPainel(Empresa empresa, boolean admin) {
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
                admin ? empresa.getGoogleCalendarId() : null,
                empresa.getLocacaoPorHora(),
                Boolean.TRUE.equals(empresa.getPermiteReservaAutomatica()),
                modulos,
                empresa.getCidadePrevisao(),
                empresa.getCidadePrevisaoResolvida(),
                Boolean.TRUE.equals(empresa.getPrevisaoTempoAtiva()),
                LocalDateTime.now()
        );
    }
}
