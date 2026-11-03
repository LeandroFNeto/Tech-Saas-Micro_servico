package com.example.demo.servico.assistente;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.model.Aviso;
import com.example.demo.model.TipoAviso;
import com.example.demo.repository.AvisoRepository;
import com.example.demo.servico.ServicoIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistenteTutoriaServiceTest {

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private ServicoIA servicoIA;

    private AssistenteTutoriaService service;

    @BeforeEach
    void setUp() {
        service = new AssistenteTutoriaService(avisoRepository, servicoIA);
    }

    @Test
    @DisplayName("Deve carregar base de conhecimento do arquivo ou classpath")
    void deveCarregarBaseConhecimento() {
        String base = service.obterBaseConhecimento();
        assertThat(base).isNotBlank();
        assertThat(base).contains("GAMB");
    }

    @Test
    @DisplayName("Deve devolver resposta da IA quando comunicacao for bem sucedida")
    void deveResponderViaIAQuandoDisponivel() {
        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of());
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn("Instruções detalhadas geradas pelo Gemini Flash.");

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Como funciona?", "DIAGNOSTICO");
        AssistenteChatResponseDTO resp = service.responder(req);

        assertThat(resp).isNotNull();
        assertThat(resp.resposta()).isEqualTo("Instruções detalhadas geradas pelo Gemini Flash.");
        assertThat(resp.modo()).isEqualTo("DIAGNOSTICO");
        assertThat(resp.sugestoes()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve acionar fallback deterministico sobre WhatsApp se IA falhar")
    void deveAcionarFallbackWhatsAppSeIAFalhar() {
        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of());
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn(null);

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Como conectar o whatsapp pelo qr code?", "DIAGNOSTICO");
        AssistenteChatResponseDTO resp = service.responder(req);

        assertThat(resp).isNotNull();
        assertThat(resp.resposta()).contains("QR Code").contains("Conexão WhatsApp");
    }

    @Test
    @DisplayName("Deve acionar fallback com dados do aviso ativo mais recente")
    void deveAcionarFallbackComAvisosAtivos() {
        Aviso aviso = new Aviso();
        aviso.setTipo(TipoAviso.MANUTENCAO);
        aviso.setTitulo("Manutenção Programada");
        aviso.setConteudo("Servidores em atualização às 23h.");
        aviso.setAtivo(true);

        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of(aviso));
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn(null);

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Tem algum aviso?", "DIAGNOSTICO");
        AssistenteChatResponseDTO resp = service.responder(req);

        assertThat(resp.resposta()).contains("Manutenção Programada").contains("Servidores em atualização");
    }

    @Test
    @DisplayName("Deve responder no modo ATENDIMENTO_SANDBOX")
    void deveResponderModoAtendimentoSandbox() {
        when(avisoRepository.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of());
        when(servicoIA.gerarRespostaHumanizada(anyString())).thenReturn(null);

        AssistenteChatRequestDTO req = new AssistenteChatRequestDTO("Oi, tudo bem?", "ATENDIMENTO_SANDBOX");
        AssistenteChatResponseDTO resp = service.responder(req);

        assertThat(resp.modo()).isEqualTo("ATENDIMENTO_SANDBOX");
        assertThat(resp.resposta()).contains("Olá! Sou o assistente virtual");
    }
}
