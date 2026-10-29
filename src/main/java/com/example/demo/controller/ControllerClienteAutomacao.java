package com.example.demo.controller;

import com.example.demo.dto.contato.ContatoResumoDTO;
import com.example.demo.dto.contato.NumeroContatoDTO;
import com.example.demo.dto.reserva.ReservaResumoDTO;
import com.example.demo.dto.reserva.VincularContatoDTO;
import com.example.demo.model.*;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.servico.ServicoAcessoEmpresa;
import com.example.demo.servico.ServicoLembreteReserva;
import com.example.demo.servico.ServicoSincronizacaoAgenda;
import com.example.demo.util.WhatsappUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/empresas/{sessao}")
@Tag(name = "Automação do Cliente", description = "Endpoints de reservas, lembretes de confirmação e contatos da empresa")
public class ControllerClienteAutomacao {

    @Autowired
    private ServicoAcessoEmpresa servicoAcessoEmpresa;

    @Autowired
    private ServicoSincronizacaoAgenda servicoSincronizacaoAgenda;

    @Autowired
    private ServicoLembreteReserva servicoLembreteReserva;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private AuditoriaAcessoContatoRepository auditoriaRepository;

    @GetMapping("/reservas")
    @Operation(summary = "Listar reservas da empresa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas retornada"),
            @ApiResponse(responseCode = "401", description = "Sem token de autenticação"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public ResponseEntity<List<ReservaResumoDTO>> listarReservas(
            @PathVariable String sessao,
            @RequestParam(defaultValue = "30") int dias,
            @RequestHeader(value = "x-admin-token", required = false) String adminToken) {

        if (dias < 1 || dias > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dias deve estar entre 1 e 60");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoExclusivoCliente(auth, adminToken, sessao);

        servicoSincronizacaoAgenda.sincronizarSeNecessario(empresa, dias);

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(fuso);
        LocalDate fim = hoje.plusDays(dias);

        List<Reserva> reservas = reservaRepository.findByEmpresaIdAndDataBetweenOrderByDataAscHorarioAsc(
                empresa.getId(), hoje, fim);

        List<ReservaResumoDTO> dtos = reservas.stream()
                .map(r -> paraReservaResumo(r, hoje))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/reservas/{id}/lembrete")
    @Operation(summary = "Enviar lembrete de confirmação de reserva agora")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lembrete enviado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Sem token de autenticação"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa ou reserva não encontrada"),
            @ApiResponse(responseCode = "409", description = "Reserva sem contato, contato não deseja mensagens ou reserva em data passada"),
            @ApiResponse(responseCode = "502", description = "Falha no envio da mensagem via WhatsApp")
    })
    public ResponseEntity<ReservaResumoDTO> enviarLembreteReserva(
            @PathVariable String sessao,
            @PathVariable Long id,
            @RequestHeader(value = "x-admin-token", required = false) String adminToken) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoExclusivoCliente(auth, adminToken, sessao);

        Reserva reserva = reservaRepository.findByIdAndEmpresaId(id, empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada"));

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(fuso);

        if (reserva.getContato() == null
                || reserva.getContato().isNaoContatar()
                || reserva.getData().isBefore(hoje)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível enviar lembrete para esta reserva");
        }

        LocalDateTime agora = LocalDateTime.now(fuso);
        boolean sucesso = servicoLembreteReserva.enviarLembrete(reserva, agora);
        if (!sucesso) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha no envio da mensagem via WhatsApp");
        }

        return ResponseEntity.ok(paraReservaResumo(reserva, hoje));
    }

    @PutMapping("/reservas/{id}/contato")
    @Operation(summary = "Vincular contato a uma reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contato vinculado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Sem token de autenticação"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa, reserva ou contato não encontrado")
    })
    public ResponseEntity<ReservaResumoDTO> vincularContatoReserva(
            @PathVariable String sessao,
            @PathVariable Long id,
            @Valid @RequestBody VincularContatoDTO dto,
            @RequestHeader(value = "x-admin-token", required = false) String adminToken) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoExclusivoCliente(auth, adminToken, sessao);

        Reserva reserva = reservaRepository.findByIdAndEmpresaId(id, empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva não encontrada"));

        if (dto.contatoId() == null && (dto.telefone() == null || dto.telefone().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe contatoId ou telefone");
        }

        Contato contato;
        if (dto.contatoId() != null) {
            contato = contatoRepository.findByIdAndEmpresaId(dto.contatoId(), empresa.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado"));
        } else {
            String tel = ServicoSincronizacaoAgenda.extrairTelefoneBrasileiro(dto.telefone());
            if (tel == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone brasileiro inválido");
            }
            contato = servicoSincronizacaoAgenda.obterOuCriarContatoPorTelefone(empresa, tel);
        }

        reserva.setContato(contato);
        reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);
        reserva.setLembreteEnviarEm(ServicoSincronizacaoAgenda.calcularLembreteEnviarEm(empresa, reserva.getData()));
        reserva.setAtualizadaEm(LocalDateTime.now());
        reservaRepository.save(reserva);

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        return ResponseEntity.ok(paraReservaResumo(reserva, hoje));
    }

    @GetMapping("/contatos")
    @Operation(summary = "Listar contatos da empresa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de contatos retornada"),
            @ApiResponse(responseCode = "401", description = "Sem token de autenticação"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public ResponseEntity<List<ContatoResumoDTO>> listarContatos(
            @PathVariable String sessao,
            @RequestHeader(value = "x-admin-token", required = false) String adminToken) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoExclusivoCliente(auth, adminToken, sessao);

        List<Contato> contatos = contatoRepository.findByEmpresaIdOrderByUltimaConversaEmDesc(empresa.getId());

        List<ContatoResumoDTO> dtos = contatos.stream()
                .map(this::paraContatoResumo)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/contatos/{id}/numero")
    @Operation(summary = "Visualizar número completo de um contato")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Número completo retornado"),
            @ApiResponse(responseCode = "401", description = "Sem token de autenticação"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa ou contato não encontrado"),
            @ApiResponse(responseCode = "429", description = "Limite de visualização de números por hora atingido")
    })
    public ResponseEntity<NumeroContatoDTO> obterNumeroCompletoContato(
            @PathVariable String sessao,
            @PathVariable Long id,
            @RequestHeader(value = "x-admin-token", required = false) String adminToken) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Empresa empresa = servicoAcessoEmpresa.validarAcessoExclusivoCliente(auth, adminToken, sessao);

        LocalDateTime umaHoraAtras = LocalDateTime.now().minusHours(1);
        long acessosNaUltimaHora = auditoriaRepository.countByEmpresaIdAndDataHoraAfter(empresa.getId(), umaHoraAtras);
        if (acessosNaUltimaHora >= 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de 10 consultas por hora atingido");
        }

        Contato contato = contatoRepository.findByIdAndEmpresaId(id, empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contato não encontrado"));

        String usuarioEmail = (auth.getPrincipal() instanceof Usuario u) ? u.getEmail() : null;
        AuditoriaAcessoContato auditoria = new AuditoriaAcessoContato(empresa, contato, LocalDateTime.now(), usuarioEmail);
        auditoriaRepository.save(auditoria);

        String numeroCompleto = WhatsappUtil.extrairApenasNumeros(contato.getWhatsappId());
        return ResponseEntity.ok(new NumeroContatoDTO(numeroCompleto));
    }

    private ReservaResumoDTO paraReservaResumo(Reserva r, LocalDate hoje) {
        Contato c = r.getContato();
        boolean podeAvisar = c != null
                && !c.isNaoContatar()
                && !r.getData().isBefore(hoje)
                && r.getSituacao() != SituacaoReserva.ENCERRADA
                && r.getSituacao() != SituacaoReserva.CONFIRMADA_PELO_CLIENTE
                && r.getSituacao() != SituacaoReserva.CANCELADA_NA_AGENDA;

        boolean riscoPerda = r.getSituacao() == SituacaoReserva.SEM_RESPOSTA;
        boolean precisaAtencao = r.getSituacao() == SituacaoReserva.FALHA_ENVIO
                || r.getSituacao() == SituacaoReserva.FALHA_AGENDA;

        return new ReservaResumoDTO(
                r.getId(),
                r.getData(),
                r.getHorario(),
                r.getTitulo(),
                c != null ? c.getNome() : null,
                r.getSituacao(),
                riscoPerda,
                precisaAtencao,
                r.getOrigem(),
                c != null ? c.getId() : null,
                c != null ? mascararTelefone(c) : null,
                podeAvisar
        );
    }

    private ContatoResumoDTO paraContatoResumo(Contato c) {
        return new ContatoResumoDTO(
                c.getId(),
                c.getNome(),
                mascararTelefone(c),
                c.getPrimeiraConversaEm(),
                c.getUltimaConversaEm(),
                c.isNaoContatar(),
                c.getOrigem()
        );
    }

    private String mascararTelefone(Contato contato) {
        if (contato == null) {
            return null;
        }
        String jid = contato.getWhatsappId();
        if (jid != null && jid.contains("@lid")) {
            return "lid:*****";
        }
        String digitos = jid != null ? WhatsappUtil.extrairApenasNumeros(jid) : "";
        String ultimos4 = contato.getFinalTelefone() != null ? contato.getFinalTelefone() : "";
        if (digitos.length() >= 12 && digitos.startsWith("55")) {
            String ddd = digitos.substring(2, 4);
            return "(" + ddd + ") *****-" + ultimos4;
        }
        if (digitos.length() >= 10 && !digitos.startsWith("55")) {
            String ddd = digitos.substring(0, 2);
            return "(" + ddd + ") *****-" + ultimos4;
        }
        return "*****-" + ultimos4;
    }
}
