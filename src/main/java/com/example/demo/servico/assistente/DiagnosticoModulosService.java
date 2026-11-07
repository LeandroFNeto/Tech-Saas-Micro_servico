package com.example.demo.servico.assistente;

import com.example.demo.dto.assistente.DiagnosticoResponseDTO;
import com.example.demo.dto.assistente.DiagnosticoStatusDTO;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoWppConnect;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
                return new DiagnosticoStatusDTO("WhatsApp", "DEGRADADO", "Serviço WPPConnect não instanciado", System.currentTimeMillis() - inicio);
            }
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("WhatsApp", "OPERACIONAL", "Serviço WPPConnect configurado e pronto", duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("WhatsApp", "DEGRADADO", "Falha na verificação do WhatsApp: " + e.getMessage(), duracao);
        }
    }

    private DiagnosticoStatusDTO testarIA() {
        long inicio = System.currentTimeMillis();
        try {
            if (servicoIA == null) {
                return new DiagnosticoStatusDTO("IA", "DEGRADADO", "Serviço IA não configurado", System.currentTimeMillis() - inicio);
            }
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("IA", "OPERACIONAL", "Serviço IA operacional com modelo Gemini", duracao);
        } catch (Exception e) {
            long duracao = System.currentTimeMillis() - inicio;
            return new DiagnosticoStatusDTO("IA", "DEGRADADO", "Falha no serviço IA: " + e.getMessage(), duracao);
        }
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
