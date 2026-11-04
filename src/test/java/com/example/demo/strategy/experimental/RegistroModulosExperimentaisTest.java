package com.example.demo.strategy.experimental;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroModulosExperimentaisTest {

    @Mock private AssistenteTutoriaService assistenteTutoriaService;
    @InjectMocks private ModuloTesteVirtual moduloTesteVirtual;

    private static ModuloExperimental modulo(String nome, String descricao) {
        return new ModuloExperimental() {
            @Override
            public String nome() {
                return nome;
            }

            @Override
            public String descricao() {
                return descricao;
            }

            @Override
            public String responderSimulado(Empresa empresaDaSessao, String texto) {
                return nome + ": " + texto;
            }
        };
    }

    @Test
    @DisplayName("O registro lista, na ordem, todos os módulos recebidos dos beans")
    void deveListarOsModulosRegistrados() {
        RegistroModulosExperimentais registro = new RegistroModulosExperimentais(
                List.of(modulo("alfa", "primeiro"), modulo("beta", "segundo")));

        assertThat(registro.listar()).extracting(ModuloExperimental::nome).containsExactly("alfa", "beta");
    }

    @Test
    @DisplayName("A busca ignora caixa e espaços e devolve vazio para nome desconhecido")
    void deveBuscarPeloNome() {
        RegistroModulosExperimentais registro = new RegistroModulosExperimentais(List.of(modulo("alfa", "primeiro")));

        assertThat(registro.buscar("  ALFA ")).isPresent();
        assertThat(registro.buscar("zeta")).isEmpty();
        assertThat(registro.buscar(null)).isEmpty();
    }

    @Test
    @DisplayName("O teste virtual é o primeiro módulo e reaproveita a lógica TESTE_VIRTUAL do serviço")
    void deveReaproveitarATesteVirtualDoServico() {
        Empresa empresa = new Empresa();
        empresa.setNome("Sessão de teste");
        when(assistenteTutoriaService.responderCliente(eq(empresa), org.mockito.ArgumentMatchers.any(AssistenteChatRequestDTO.class)))
                .thenReturn(new AssistenteChatResponseDTO("resposta simulada", "TESTE_VIRTUAL", Instant.now(), List.of()));

        String resposta = moduloTesteVirtual.responderSimulado(empresa, "Quais os horários?");

        ArgumentCaptor<AssistenteChatRequestDTO> captor = ArgumentCaptor.forClass(AssistenteChatRequestDTO.class);
        verify(assistenteTutoriaService).responderCliente(eq(empresa), captor.capture());
        assertThat(captor.getValue().modo()).isEqualTo("TESTE_VIRTUAL");
        assertThat(captor.getValue().mensagem()).isEqualTo("Quais os horários?");
        assertThat(resposta).isEqualTo("resposta simulada");
        assertThat(new RegistroModulosExperimentais(List.of(moduloTesteVirtual)).listar())
                .extracting(ModuloExperimental::nome).containsExactly(ModuloTesteVirtual.NOME);
    }
}
