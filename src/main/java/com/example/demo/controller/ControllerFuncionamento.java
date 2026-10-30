package com.example.demo.controller;

import com.example.demo.dto.empresa.DataEspecialCreateDTO;
import com.example.demo.dto.empresa.DataEspecialDTO;
import com.example.demo.dto.empresa.FuncionamentoDTO;
import com.example.demo.dto.empresa.FuncionamentoUpdateDTO;
import com.example.demo.dto.empresa.HorarioFuncionamentoDTO;
import com.example.demo.model.DataEspecial;
import com.example.demo.model.Empresa;
import com.example.demo.model.HorarioFuncionamento;
import com.example.demo.model.PerfilFuncionamento;
import com.example.demo.repository.DataEspecialRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.HorarioFuncionamentoRepository;
import com.example.demo.servico.CalendarioFeriados;
import com.example.demo.servico.ServicoAcessoEmpresa;
import com.example.demo.servico.ServicoFuncionamento;
import com.example.demo.servico.ServicoGoogleagenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/empresas/{sessao}")
@Tag(name = "Funcionamento da reserva por hora", description = "Duração, horários por perfil, feriado e dias especiais da empresa")
public class ControllerFuncionamento {

    private static final Logger log = LoggerFactory.getLogger(ControllerFuncionamento.class);

    @Autowired private ServicoAcessoEmpresa servicoAcessoEmpresa;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private HorarioFuncionamentoRepository horarioRepository;
    @Autowired private DataEspecialRepository dataEspecialRepository;
    @Autowired private ServicoGoogleagenda servicoGoogleagenda;

    @GetMapping("/funcionamento")
    @Operation(summary = "Ler o funcionamento da reserva por hora")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionamento atual, com os padrões quando nada foi configurado"),
            @ApiResponse(responseCode = "401", description = "Sem token"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada (admin)")
    })
    public ResponseEntity<FuncionamentoDTO> ler(
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @Parameter(description = "Identificador da sessão WhatsApp da empresa", example = "sessao_recanto_01")
            @PathVariable String sessao) {
        Empresa empresa = acessar(adminToken, sessao);
        return ResponseEntity.ok(montar(empresa));
    }

    @PutMapping("/funcionamento")
    @Operation(summary = "Atualizar o funcionamento da reserva por hora")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionamento atualizado"),
            @ApiResponse(responseCode = "400", description = "Duração, intervalo, UF ou horário inválido"),
            @ApiResponse(responseCode = "401", description = "Sem token"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada (admin)")
    })
    @Transactional
    public ResponseEntity<FuncionamentoDTO> atualizar(
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @PathVariable String sessao,
            @RequestBody FuncionamentoUpdateDTO dto) {
        Empresa empresa = acessar(adminToken, sessao);

        if (dto.duracaoAtendimentoMinutos() != null) {
            exigirDuracao(dto.duracaoAtendimentoMinutos());
        }
        if (dto.intervaloEntreAtendimentosMinutos() != null
                && (dto.intervaloEntreAtendimentosMinutos() < 0 || dto.intervaloEntreAtendimentosMinutos() > 120)) {
            throw requisicaoInvalida("O intervalo entre atendimentos vai de 0 a 120 minutos.");
        }
        String uf = null;
        boolean alterarUf = dto.uf() != null;
        if (alterarUf && !dto.uf().isBlank()) {
            if (!CalendarioFeriados.ufValida(dto.uf().trim())) {
                throw requisicaoInvalida("Estado inválido. Use a sigla, como SP.");
            }
            uf = dto.uf().trim().toUpperCase();
        }
        List<HorarioFuncionamento> novos = null;
        if (dto.horarios() != null) {
            int duracaoDaEmpresa = dto.duracaoAtendimentoMinutos() != null
                    ? dto.duracaoAtendimentoMinutos()
                    : empresa.duracaoEfetivaMinutos();
            novos = validarHorarios(empresa, duracaoDaEmpresa, dto.horarios());
        }

        if (dto.duracaoAtendimentoMinutos() != null) {
            empresa.setDuracaoAtendimentoMinutos(dto.duracaoAtendimentoMinutos());
        }
        if (dto.intervaloEntreAtendimentosMinutos() != null) {
            empresa.setIntervaloEntreAtendimentosMinutos(dto.intervaloEntreAtendimentosMinutos());
        }
        if (alterarUf) {
            empresa.setUf(uf);
        }
        empresaRepository.save(empresa);
        if (novos != null) {
            horarioRepository.deleteByEmpresaId(empresa.getId());
            horarioRepository.saveAll(novos);
        }
        return ResponseEntity.ok(montar(empresa));
    }

    @PostMapping("/dias-especiais")
    @Operation(summary = "Cadastrar um dia especial",
            description = "Feriado municipal, folga ou horário próprio. Fechar uma data não cancela reservas; a resposta lista os eventos que já existem.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dia especial cadastrado"),
            @ApiResponse(responseCode = "400", description = "Data fora de hoje até 180 dias, horário inválido ou data repetida"),
            @ApiResponse(responseCode = "401", description = "Sem token"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada (admin)")
    })
    public ResponseEntity<DataEspecialDTO> cadastrarDiaEspecial(
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @PathVariable String sessao,
            @RequestBody DataEspecialCreateDTO dto) {
        Empresa empresa = acessar(adminToken, sessao);

        LocalDate data = lerData(dto.data());
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE))) {
            throw requisicaoInvalida("A data precisa estar entre hoje e " + ServicoFuncionamento.DIAS_MAXIMOS_A_FRENTE
                    + " dias à frente.");
        }
        if (dataEspecialRepository.findByEmpresaIdAndData(empresa.getId(), data).isPresent()) {
            throw requisicaoInvalida("Essa data já tem um dia especial. Remova o anterior antes.");
        }

        DataEspecial especial = new DataEspecial();
        especial.setEmpresa(empresa);
        especial.setData(data);
        especial.setFechado(dto.fechado());
        if (!dto.fechado()) {
            if ("24:00".equals(dto.abre() != null ? dto.abre().trim() : "")) {
                throw requisicaoInvalida("Informe abertura e fechamento, com a abertura antes do fechamento.");
            }
            LocalTime abre = lerHora(dto.abre());
            LocalTime fecha = lerHora(dto.fecha());
            if (abre == null || fecha == null || !abreAntesDeFecha(abre, fecha)) {
                throw requisicaoInvalida("Informe abertura e fechamento, com a abertura antes do fechamento.");
            }
            exigirJanelaComporta(abre, fecha, empresa.duracaoEfetivaMinutos());
            especial.setAbre(abre);
            especial.setFecha(fecha);
        }
        if (dto.motivo() != null && !dto.motivo().isBlank()) {
            String motivo = dto.motivo().trim();
            especial.setMotivo(motivo.substring(0, Math.min(motivo.length(), 120)));
        }
        DataEspecial salvo;
        try {
            salvo = dataEspecialRepository.save(especial);
        } catch (DataIntegrityViolationException e) {
            throw requisicaoInvalida("Essa data já tem um dia especial. Remova o anterior antes.");
        }

        List<String> reservas = dto.fechado() ? reservasDaData(empresa, data) : List.of();
        return ResponseEntity.status(HttpStatus.CREATED).body(paraDto(salvo, reservas));
    }

    @DeleteMapping("/dias-especiais/{id}")
    @Operation(summary = "Remover um dia especial")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dia especial removido"),
            @ApiResponse(responseCode = "401", description = "Sem token"),
            @ApiResponse(responseCode = "403", description = "Cliente de outra empresa"),
            @ApiResponse(responseCode = "404", description = "Dia especial não encontrado")
    })
    public ResponseEntity<Void> removerDiaEspecial(
            @RequestHeader(value = "x-admin-token", required = false) String adminToken,
            @PathVariable String sessao,
            @PathVariable Long id) {
        Empresa empresa = acessar(adminToken, sessao);
        DataEspecial especial = dataEspecialRepository.findByIdAndEmpresaId(id, empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dia especial não encontrado"));
        dataEspecialRepository.delete(especial);
        return ResponseEntity.noContent().build();
    }

    private Empresa acessar(String adminToken, String sessao) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return servicoAcessoEmpresa.validarAcessoSessao(autenticacao, adminToken, sessao);
    }

    private FuncionamentoDTO montar(Empresa empresa) {
        List<HorarioFuncionamentoDTO> horarios = horarioRepository.findByEmpresaId(empresa.getId()).stream()
                .sorted(Comparator.comparing(HorarioFuncionamento::getPerfil))
                .map(h -> new HorarioFuncionamentoDTO(
                        h.getPerfil(),
                        h.isFechado(),
                        h.getAbre() == null ? null : h.getAbre().toString(),
                        h.getFecha() == null ? null : (h.getFecha().equals(LocalTime.MIDNIGHT) ? "24:00" : h.getFecha().toString()),
                        h.getDuracaoMinutos()))
                .toList();
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        List<DataEspecialDTO> especiais = dataEspecialRepository
                .findByEmpresaIdAndDataGreaterThanEqualOrderByData(empresa.getId(), hoje).stream()
                .map(e -> paraDto(e, List.of()))
                .toList();
        return new FuncionamentoDTO(
                empresa.duracaoEfetivaMinutos(),
                empresa.intervaloEfetivoMinutos(),
                empresa.getUf(),
                horarios,
                especiais);
    }

    private DataEspecialDTO paraDto(DataEspecial e, List<String> reservas) {
        return new DataEspecialDTO(
                e.getId(),
                e.getData().toString(),
                e.isFechado(),
                e.getAbre() == null ? null : e.getAbre().toString(),
                e.getFecha() == null ? null : (e.getFecha().equals(LocalTime.MIDNIGHT) ? "24:00" : e.getFecha().toString()),
                e.getMotivo(),
                reservas);
    }

    private void exigirJanelaComporta(LocalTime abre, LocalTime fecha, int duracaoMinutos) {
        long minutosDisponiveis = fecha.equals(LocalTime.MIDNIGHT)
                ? Duration.ofHours(24).minusMinutes(abre.getHour() * 60L + abre.getMinute()).toMinutes()
                : Duration.between(abre, fecha).toMinutes();
        if (minutosDisponiveis < duracaoMinutos) {
            throw requisicaoInvalida("O horário de funcionamento precisa comportar ao menos um atendimento de "
                    + duracaoMinutos + " minutos.");
        }
    }

    private List<HorarioFuncionamento> validarHorarios(Empresa empresa, int duracaoDaEmpresa,
                                                       List<HorarioFuncionamentoDTO> itens) {
        Set<PerfilFuncionamento> vistos = EnumSet.noneOf(PerfilFuncionamento.class);
        List<HorarioFuncionamento> resultado = new ArrayList<>();
        for (HorarioFuncionamentoDTO item : itens) {
            if (item.perfil() == null || !vistos.add(item.perfil())) {
                throw requisicaoInvalida("Cada perfil de horário pode aparecer uma vez.");
            }
            HorarioFuncionamento linha = new HorarioFuncionamento();
            linha.setEmpresa(empresa);
            linha.setPerfil(item.perfil());
            linha.setFechado(item.fechado());
            if (!item.fechado()) {
                if ("24:00".equals(item.abre() != null ? item.abre().trim() : "")) {
                    throw requisicaoInvalida("Informe abertura e fechamento, com a abertura antes do fechamento.");
                }
                LocalTime abre = lerHora(item.abre());
                LocalTime fecha = lerHora(item.fecha());
                if (abre == null || fecha == null || !abreAntesDeFecha(abre, fecha)) {
                    throw requisicaoInvalida("Informe abertura e fechamento, com a abertura antes do fechamento.");
                }
                linha.setAbre(abre);
                linha.setFecha(fecha);
            }
            if (item.duracaoMinutos() != null) {
                exigirDuracao(item.duracaoMinutos());
                linha.setDuracaoMinutos(item.duracaoMinutos());
            }
            if (!item.fechado()) {
                int duracao = item.duracaoMinutos() != null ? item.duracaoMinutos() : duracaoDaEmpresa;
                exigirJanelaComporta(linha.getAbre(), linha.getFecha(), duracao);
            }
            resultado.add(linha);
        }
        return resultado;
    }

    private void exigirDuracao(int minutos) {
        if (minutos < 30 || minutos > 720 || minutos % 30 != 0) {
            throw requisicaoInvalida("A duração do atendimento vai de 30 a 720 minutos, de 30 em 30.");
        }
    }

    private LocalDate lerData(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw requisicaoInvalida("Data inválida. Use o formato AAAA-MM-DD.");
        }
    }

    private LocalTime lerHora(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        if ("24:00".equals(texto.trim())) {
            return LocalTime.MIDNIGHT;
        }
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw requisicaoInvalida("Hora inválida. Use o formato HH:mm.");
        }
    }

    private boolean abreAntesDeFecha(LocalTime abre, LocalTime fecha) {
        if (abre == null || fecha == null) {
            return false;
        }
        if (fecha.equals(LocalTime.MIDNIGHT)) {
            return !abre.equals(LocalTime.MIDNIGHT);
        }
        return abre.isBefore(fecha);
    }

    private List<String> reservasDaData(Empresa empresa, LocalDate data) {
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId == null || calendarId.isBlank()) {
            return List.of();
        }
        try {
            return servicoGoogleagenda.resumosDoDia(calendarId, data);
        } catch (Exception e) {
            log.warn("Não foi possível listar as reservas da data fechada (empresa {}): {}",
                    empresa.getId(), e.getClass().getSimpleName());
            return List.of();
        }
    }

    private ResponseStatusException requisicaoInvalida(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
