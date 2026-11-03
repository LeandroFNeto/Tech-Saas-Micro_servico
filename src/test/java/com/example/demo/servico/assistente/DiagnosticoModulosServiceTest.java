package com.example.demo.servico.assistente;

import com.example.demo.dto.assistente.DiagnosticoResponseDTO;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.servico.ServicoIA;
import com.example.demo.servico.ServicoWppConnect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticoModulosServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private ServicoWppConnect servicoWppConnect;

    @Mock
    private ServicoIA servicoIA;

    private DiagnosticoModulosService service;

    @BeforeEach
    void setUp() {
        service = new DiagnosticoModulosService(
                usuarioRepository,
                avisoRepository,
                reservaRepository,
                empresaRepository,
                servicoWppConnect,
                servicoIA
        );
    }

    @Test
    @DisplayName("Deve retornar status geral OPERACIONAL quando todos os modulos estiverem saudaveis")
    void deveRetornarOperacionalQuandoTudoSaudavel() {
        when(usuarioRepository.count()).thenReturn(5L);
        when(avisoRepository.count()).thenReturn(2L);
        when(reservaRepository.count()).thenReturn(10L);
        when(empresaRepository.count()).thenReturn(1L);

        DiagnosticoResponseDTO resultado = service.executarDiagnostico();

        assertThat(resultado).isNotNull();
        assertThat(resultado.statusGeral()).isEqualTo("OPERACIONAL");
        assertThat(resultado.modulos()).hasSize(6);
        assertThat(resultado.modulos()).allMatch(m -> "OPERACIONAL".equals(m.status()));
    }

    @Test
    @DisplayName("Deve sinalizar status CRITICO se algum modulo de banco falhar")
    void deveRetornarCriticoSeModuloFalhar() {
        when(usuarioRepository.count()).thenThrow(new RuntimeException("Conexao recusada"));
        when(avisoRepository.count()).thenReturn(2L);
        when(reservaRepository.count()).thenReturn(10L);
        when(empresaRepository.count()).thenReturn(1L);

        DiagnosticoResponseDTO resultado = service.executarDiagnostico();

        assertThat(resultado).isNotNull();
        assertThat(resultado.statusGeral()).isEqualTo("CRITICO");
        assertThat(resultado.modulos().stream()
                .filter(m -> "Auth".equals(m.modulo()))
                .findFirst()
                .orElseThrow().status()).isEqualTo("INDISPONIVEL");
    }

    @Test
    @DisplayName("Deve sinalizar status ATENCAO se modulo externo estiver degradado")
    void deveRetornarAtencaoSeModuloExternoDegradado() {
        when(usuarioRepository.count()).thenReturn(5L);
        when(avisoRepository.count()).thenReturn(2L);
        when(reservaRepository.count()).thenReturn(10L);
        when(empresaRepository.count()).thenReturn(1L);

        DiagnosticoModulosService serviceSemWpp = new DiagnosticoModulosService(
                usuarioRepository,
                avisoRepository,
                reservaRepository,
                empresaRepository,
                null,
                servicoIA
        );

        DiagnosticoResponseDTO resultado = serviceSemWpp.executarDiagnostico();

        assertThat(resultado).isNotNull();
        assertThat(resultado.statusGeral()).isEqualTo("ATENCAO");
        assertThat(resultado.modulos().stream()
                .filter(m -> "WhatsApp".equals(m.modulo()))
                .findFirst()
                .orElseThrow().status()).isEqualTo("DEGRADADO");
    }
}
