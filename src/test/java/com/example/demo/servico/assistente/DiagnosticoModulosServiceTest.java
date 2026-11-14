package com.example.demo.servico.assistente;

import com.example.demo.servico.GuardaCotaIA;
import com.example.demo.dto.assistente.DiagnosticoResponseDTO;
import com.example.demo.model.Empresa;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
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
        lenient().when(servicoWppConnect.consultarSaude()).thenReturn(saude(ServicoWppConnect.SaudeGateway.Situacao.OK, 2, List.of()));
    }

    private ServicoWppConnect.SaudeGateway saude(ServicoWppConnect.SaudeGateway.Situacao situacao, int conectadas, List<String> desconectadas) {
        return new ServicoWppConnect.SaudeGateway(situacao, conectadas, 0, desconectadas, null);
    }

    private ServicoWppConnect.SaudeGateway saudeComConectadas(List<String> nomesConectadas) {
        return new ServicoWppConnect.SaudeGateway(ServicoWppConnect.SaudeGateway.Situacao.OK, nomesConectadas.size(), 0, List.of(), nomesConectadas);
    }

    private Empresa empresa(String sessao, Boolean ativo) {
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp(sessao);
        empresa.setAtivo(ativo);
        return empresa;
    }

    private com.example.demo.dto.assistente.DiagnosticoStatusDTO whatsapp() {
        when(usuarioRepository.count()).thenReturn(5L);
        when(avisoRepository.count()).thenReturn(2L);
        when(reservaRepository.count()).thenReturn(10L);
        when(empresaRepository.count()).thenReturn(1L);
        return service.executarDiagnostico().modulos().stream()
                .filter(m -> "WhatsApp".equals(m.modulo()))
                .findFirst()
                .orElseThrow();
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

    @Test
    @DisplayName("Mostra a IA como degradada quando a guarda de cota suspende o modelo")
    void iaSuspensaPelaGuardaFicaDegradada() {
        when(usuarioRepository.count()).thenReturn(5L);
        when(avisoRepository.count()).thenReturn(2L);
        when(reservaRepository.count()).thenReturn(10L);
        when(empresaRepository.count()).thenReturn(1L);
        when(servicoIA.situacaoCota()).thenReturn(GuardaCotaIA.Situacao.TETO_GASTO_DIARIO);

        DiagnosticoResponseDTO resultado = service.executarDiagnostico();

        assertThat(resultado.statusGeral()).isEqualTo("ATENCAO");
        assertThat(resultado.modulos()).filteredOn(m -> "IA".equals(m.modulo()))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.status()).isEqualTo("DEGRADADO");
                    assertThat(m.mensagem()).contains("Teto diário de gasto");
                });
    }

    @Test
    @DisplayName("WhatsApp fica INDISPONIVEL quando o gateway não responde")
    void whatsappIndisponivelSemRespostaDoGateway() {
        when(servicoWppConnect.consultarSaude()).thenReturn(saude(ServicoWppConnect.SaudeGateway.Situacao.INDISPONIVEL, 0, List.of()));

        var modulo = whatsapp();

        assertThat(modulo.status()).isEqualTo("INDISPONIVEL");
        assertThat(service.executarDiagnostico().statusGeral()).isEqualTo("CRITICO");
    }

    @Test
    @DisplayName("WhatsApp fica DEGRADADO com sessão de empresa ativa desconectada, listando só os nomes")
    void whatsappDegradadoComSessaoAtivaDesconectada() {
        when(servicoWppConnect.consultarSaude()).thenReturn(
                saude(ServicoWppConnect.SaudeGateway.Situacao.OK, 1, List.of("sessao_a", "sessao_inativa", "sessao_gamb_teste")));
        when(empresaRepository.findAll()).thenReturn(List.of(
                empresa("sessao_a", true), empresa("sessao_inativa", false), empresa("sessao_b", null)));

        var modulo = whatsapp();

        assertThat(modulo.status()).isEqualTo("DEGRADADO");
        assertThat(modulo.mensagem()).contains("1 sessão(ões)").contains("sessao_a")
                .doesNotContain("sessao_inativa").doesNotContain("sessao_gamb_teste");
    }

    @Test
    @DisplayName("WhatsApp fica OPERACIONAL quando só sessão de empresa inativa está desconectada")
    void whatsappOperacionalSeDesconectadaEhDeEmpresaInativa() {
        when(servicoWppConnect.consultarSaude()).thenReturn(
                saude(ServicoWppConnect.SaudeGateway.Situacao.OK, 1, List.of("sessao_inativa")));
        when(empresaRepository.findAll()).thenReturn(List.of(empresa("sessao_inativa", false)));

        assertThat(whatsapp().status()).isEqualTo("OPERACIONAL");
    }

    @Test
    @DisplayName("Gateway sem rota de saúde (WPPConnect) fica OPERACIONAL com a mensagem de sempre")
    void whatsappOperacionalSemRotaDeSaude() {
        when(servicoWppConnect.consultarSaude()).thenReturn(saude(ServicoWppConnect.SaudeGateway.Situacao.SEM_ROTA, 0, List.of()));

        var modulo = whatsapp();

        assertThat(modulo.status()).isEqualTo("OPERACIONAL");
        assertThat(modulo.mensagem()).isEqualTo("gateway sem rota de saúde");
    }

    @Test
    @DisplayName("WhatsApp fica OPERACIONAL com todas as sessões conectadas")
    void whatsappOperacionalComTodasConectadas() {
        var modulo = whatsapp();

        assertThat(modulo.status()).isEqualTo("OPERACIONAL");
        assertThat(modulo.mensagem()).contains("2 sessão(ões) conectada(s)");
    }

    @Test
    @DisplayName("WhatsApp fica DEGRADADO com empresa ativa fora da lista de conectadas, inclusive sessão aguardando QR ou desconhecida do gateway")
    void whatsappDegradadoComAtivaForaDasConectadas() {
        when(servicoWppConnect.consultarSaude()).thenReturn(saudeComConectadas(List.of("sessao_a")));
        when(empresaRepository.findAll()).thenReturn(List.of(
                empresa("sessao_a", true), empresa("sessao_qr", true), empresa("sessao_desconhecida", true), empresa("sessao_inativa", false)));

        var modulo = whatsapp();

        assertThat(modulo.status()).isEqualTo("DEGRADADO");
        assertThat(modulo.mensagem()).contains("2 sessão(ões)").contains("sessao_desconhecida").contains("sessao_qr")
                .doesNotContain("sessao_inativa").doesNotContain("sessao_a,");
    }

    @Test
    @DisplayName("WhatsApp fica OPERACIONAL quando todas as empresas ativas estão entre as conectadas")
    void whatsappOperacionalComAtivasTodasConectadas() {
        when(servicoWppConnect.consultarSaude()).thenReturn(saudeComConectadas(List.of("sessao_a", "sessao_extra")));
        when(empresaRepository.findAll()).thenReturn(List.of(empresa("sessao_a", true)));

        assertThat(whatsapp().status()).isEqualTo("OPERACIONAL");
    }
}
