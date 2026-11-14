package com.example.demo.servico.assistente;

import com.example.demo.dto.assistente.DiagnosticoResponseDTO;
import com.example.demo.dto.assistente.DiagnosticoStatusDTO;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.GuardaCotaIA;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoWppConnect;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiagnosticoModulosService {

    private final UsuarioRepository usuarioRepository;
    private final AvisoRepository avisoRepository;
    private final ReservaRepository reservaRepository;
    private final EmpresaRepository empresaRepository;
    private final ServicoWppConnect servicoWppConnect;
    private final ServicoIA servicoIA;

    public DiagnosticoModulosService(
            UsuarioRepository usuarioRepository,
            AvisoRepository avisoRepository,
            ReservaRepository reservaRepository,
            EmpresaRepository empresaRepository,
            ServicoWppConnect servicoWppConnect,
            ServicoIA servicoIA) {
        this.usuarioRepository = usuarioRepository;
        this.avisoRepository = avisoRepository;
        this.reservaRepository = reservaRepository;
        this.empresaRepository = empresaRepository;
        this.servicoWppConnect = servicoWppConnect;
        this.servicoIA = servicoIA;
    }

    public DiagnosticoResponseDTO executarDiagnostico() {
        List<DiagnosticoStatusDTO> modulos = new ArrayList<>();

        modulos.add(testarAuth());
        modulos.add(testarAvisos());
        modulos.add(testarAgenda());
        modulos.add(testarEmpresa());
        modulos.add(testarWhatsApp());
        modulos.add(testarIA());

        String statusGeral = calcularStatusGeral(modulos);
        return new DiagnosticoResponseDTO(statusGeral, Instant.now(), modulos);
    }

    private DiagnosticoStatusDTO testarAuth() {
        long inicio = System.currentTimeMillis();
        try {
            long total = usuarioRepository != null ? usuarioRepository.count() : 0L;
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Auth", "OPERACIONAL", "Módulo de autenticação ativo. Usuários registrados: " + total, duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Auth", "INDISPONIVEL", "Falha no repositório de usuários: " + e.getMessage(), duracao);
        }
    }

    private DiagnosticoStatusDTO testarAvisos() {
        long inicio = System.currentTimeMillis();
        try {
            long total = avisoRepository != null ? avisoRepository.count() : 0L;
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Avisos", "OPERACIONAL", "Módulo de comunicados ativo. Total de avisos: " + total, duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Avisos", "INDISPONIVEL", "Falha no repositório de avisos: " + e.getMessage(), duracao);
        }
    }

    private DiagnosticoStatusDTO testarAgenda() {
        long inicio = System.currentTimeMillis();
        try {
            long total = reservaRepository != null ? reservaRepository.count() : 0L;
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Agenda", "OPERACIONAL", "Módulo de reservas operacional. Total histórico: " + total, duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Agenda", "INDISPONIVEL", "Falha no repositório de reservas: " + e.getMessage(), duracao);
        }
    }

    private DiagnosticoStatusDTO testarEmpresa() {
        long inicio = System.currentTimeMillis();
        try {
            long total = empresaRepository != null ? empresaRepository.count() : 0L;
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Empresa", "OPERACIONAL", "Módulo de empresas operacional. Estabelecimentos: " + total, duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("Empresa", "INDISPONIVEL", "Falha no repositório de empresas: " + e.getMessage(), duracao);
        }
    }

    private DiagnosticoStatusDTO testarWhatsApp() {
        long inicio = System.currentTimeMillis();
        try {
            if (servicoWppConnect == null) {
                return new DiagnosticoStatusDTO("WhatsApp", "DEGRADADO", "Serviço do gateway não instanciado", System.currentTimeMillis() - inicio);
            }
            ServicoWppConnect.SaudeGateway saude = servicoWppConnect.consultarSaude();
            long duracao = System.currentTimeMillis() - inicio;
            if (saude == null || saude.situacao() == ServicoWppConnect.SaudeGateway.Situacao.INDISPONIVEL) {
                return new DiagnosticoStatusDTO("WhatsApp", "INDISPONIVEL", "Gateway do WhatsApp não responde", duracao);
            }
            if (saude.situacao() == ServicoWppConnect.SaudeGateway.Situacao.SEM_ROTA) {
                return new DiagnosticoStatusDTO("WhatsApp", "OPERACIONAL", "gateway sem rota de saúde", duracao);
            }
            List<String> fora = sessoesAtivasDesconectadas(saude);
            if (!fora.isEmpty()) {
                return new DiagnosticoStatusDTO("WhatsApp", "DEGRADADO",
                        fora.size() + " sessão(ões) de empresa ativa desconectada(s): " + String.join(", ", fora), duracao);
            }
            return new DiagnosticoStatusDTO("WhatsApp", "OPERACIONAL",
                    "Gateway do WhatsApp operacional, " + saude.conectadas() + " sessão(ões) conectada(s)", duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("WhatsApp", "DEGRADADO", "Falha na verificação do WhatsApp", duracao);
        }
    }

    private List<String> sessoesAtivasDesconectadas(ServicoWppConnect.SaudeGateway saude) {
        if (empresaRepository == null) {
            return List.of();
        }
        Set<String> ativas = empresaRepository.findAll().stream()
                .filter(empresa -> !Boolean.FALSE.equals(empresa.getAtivo()))
                .map(empresa -> empresa.getSessaoWhatsapp())
                .filter(sessao -> sessao != null && !sessao.isBlank())
                .collect(Collectors.toSet());
        if (saude.nomesConectadas() != null) {
            Set<String> conectadas = Set.copyOf(saude.nomesConectadas());
            return ativas.stream().filter(sessao -> !conectadas.contains(sessao)).sorted().toList();
        }
        return saude.desconectadas().stream().filter(ativas::contains).sorted().toList();
    }

    private DiagnosticoStatusDTO testarIA() {
        long inicio = System.currentTimeMillis();
        try {
            if (servicoIA == null) {
                return new DiagnosticoStatusDTO("IA", "DEGRADADO", "Serviço IA não configurado", System.currentTimeMillis() - inicio);
            }
            String suspensao = motivoSuspensao(servicoIA.situacaoCota());
            long duracao = System.currentTimeMillis() - inicio;
            if (suspensao != null) {
                return new DiagnosticoStatusDTO("IA", "DEGRADADO", suspensao, duracao);
            }
            return new DiagnosticoStatusDTO("IA", "OPERACIONAL", "Serviço IA operacional com modelo Gemini", duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("IA", "DEGRADADO", "Falha no serviço IA: " + e.getMessage(), duracao);
        }
    }

    private String motivoSuspensao(GuardaCotaIA.Situacao situacao) {
        if (situacao == null) {
            return null;
        }
        return switch (situacao) {
            case LIVRE -> null;
            case PAUSADA_APOS_429 -> "Modelo pausado após erro de cota (429); o atendimento segue pelo fallback";
            case TETO_GASTO_DIARIO -> "Teto diário de gasto da IA atingido; o modelo volta no próximo dia";
            case TETO_CHAMADAS_DIA -> "Teto diário de chamadas da IA atingido; o modelo volta no próximo dia";
            case TETO_CHAMADAS_MINUTO -> "Teto de chamadas por minuto da IA atingido; o modelo volta no minuto seguinte";
        };
    }

    private String calcularStatusGeral(List<DiagnosticoStatusDTO> modulos) {
        boolean temIndisponivel = modulos.stream().anyMatch(m -> "INDISPONIVEL".equalsIgnoreCase(m.status()));
        if (temIndisponivel) {
            return "CRITICO";
        }
        boolean temDegradado = modulos.stream().anyMatch(m -> "DEGRADADO".equalsIgnoreCase(m.status()));
        if (temDegradado) {
            return "ATENCAO";
        }
        return "OPERACIONAL";
    }
}
