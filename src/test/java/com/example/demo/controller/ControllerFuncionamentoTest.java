package com.example.demo.controller;

import com.example.demo.model.DataEspecial;
import com.example.demo.model.Empresa;
import com.example.demo.repository.DataEspecialRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.HorarioFuncionamentoRepository;
import com.example.demo.servico.ServicoAcessoEmpresa;
import com.example.demo.servico.ServicoGoogleagenda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerFuncionamento.class)
@AutoConfigureMockMvc(addFilters = false)
class ControllerFuncionamentoTest {

    private static final String SESSAO = "sessao_recanto_01";
    private static final String BASE = "/empresas/" + SESSAO;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ServicoAcessoEmpresa servicoAcessoEmpresa;
    @MockitoBean private EmpresaRepository empresaRepository;
    @MockitoBean private HorarioFuncionamentoRepository horarioRepository;
    @MockitoBean private DataEspecialRepository dataEspecialRepository;
    @MockitoBean private ServicoGoogleagenda servicoGoogleagenda;

    private Empresa empresa;

    @BeforeEach
    void preparar() {
        empresa = new Empresa();
        empresa.setId(7L);
        empresa.setSessaoWhatsapp(SESSAO);
        empresa.setGoogleCalendarId("agenda@group.calendar.google.com");
        when(servicoAcessoEmpresa.validarAcessoSessao(any(), any(), eq(SESSAO))).thenReturn(empresa);
        when(horarioRepository.findByEmpresaId(7L)).thenReturn(List.of());
        when(dataEspecialRepository.findByEmpresaIdAndDataGreaterThanEqualOrderByData(eq(7L), any()))
                .thenReturn(List.of());
    }

    private String hoje(int dias) {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(dias).toString();
    }

    @Test
    @DisplayName("GET /funcionamento devolve os padrões quando nada foi configurado")
    void deveDevolverOsPadroes() throws Exception {
        mockMvc.perform(get(BASE + "/funcionamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracaoAtendimentoMinutos").value(60))
                .andExpect(jsonPath("$.intervaloEntreAtendimentosMinutos").value(0))
                .andExpect(jsonPath("$.horarios").isEmpty())
                .andExpect(jsonPath("$.diasEspeciais").isEmpty());
    }

    @Test
    @DisplayName("GET /funcionamento de empresa alheia repassa o 403 do acesso")
    void deveRepassarOAcessoNegado() throws Exception {
        when(servicoAcessoEmpresa.validarAcessoSessao(any(), any(), eq("outra")))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta empresa"));

        mockMvc.perform(get("/empresas/outra/funcionamento")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /funcionamento grava duração, intervalo, UF e a lista de horários")
    void deveAtualizarOFuncionamento() throws Exception {
        String corpo = """
                {"duracaoAtendimentoMinutos":90,"intervaloEntreAtendimentosMinutos":15,"uf":"sp",
                 "horarios":[
                   {"perfil":"SABADO","fechado":false,"abre":"10:00","fecha":"18:00","duracaoMinutos":120},
                   {"perfil":"FERIADO","fechado":true}]}
                """;

        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracaoAtendimentoMinutos").value(90))
                .andExpect(jsonPath("$.intervaloEntreAtendimentosMinutos").value(15))
                .andExpect(jsonPath("$.uf").value("SP"));

        verify(empresaRepository).save(empresa);
        verify(horarioRepository).deleteByEmpresaId(7L);
        verify(horarioRepository).saveAll(any());
    }

    @Test
    @DisplayName("PUT /funcionamento recusa duração que não é múltipla de 30")
    void deveRecusarDuracaoInvalida() throws Exception {
        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoAtendimentoMinutos\":45}"))
                .andExpect(status().isBadRequest());
        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /funcionamento recusa intervalo acima de 120 minutos")
    void deveRecusarIntervaloInvalido() throws Exception {
        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intervaloEntreAtendimentosMinutos\":121}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /funcionamento recusa UF inexistente")
    void deveRecusarUfInvalida() throws Exception {
        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uf\":\"XX\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /funcionamento recusa abertura depois do fechamento")
    void deveRecusarAberturaDepoisDoFechamento() throws Exception {
        String corpo = "{\"horarios\":[{\"perfil\":\"SEGUNDA\",\"fechado\":false,\"abre\":\"20:00\",\"fecha\":\"08:00\"}]}";

        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        verify(horarioRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("PUT /funcionamento recusa o mesmo perfil duas vezes")
    void deveRecusarPerfilRepetido() throws Exception {
        String corpo = "{\"horarios\":[{\"perfil\":\"SEGUNDA\",\"fechado\":true},{\"perfil\":\"SEGUNDA\",\"fechado\":true}]}";

        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /funcionamento com UF em branco remove a UF")
    void deveLimparAUf() throws Exception {
        empresa.setUf("SP");

        mockMvc.perform(put(BASE + "/funcionamento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uf\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uf").doesNotExist());
    }

    @Test
    @DisplayName("POST /dias-especiais fecha a data e devolve as reservas que já existem nela")
    void deveFecharDataEAvisarDasReservas() throws Exception {
        String data = hoje(10);
        when(dataEspecialRepository.findByEmpresaIdAndData(eq(7L), any())).thenReturn(Optional.empty());
        when(dataEspecialRepository.save(any(DataEspecial.class))).thenAnswer(i -> i.getArgument(0));
        when(servicoGoogleagenda.resumosDoDia(anyString(), any())).thenReturn(List.of("14:00 às 15:00 - Reserva - Maria"));

        mockMvc.perform(post(BASE + "/dias-especiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + data + "\",\"fechado\":true,\"motivo\":\"Feriado municipal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechado").value(true))
                .andExpect(jsonPath("$.motivo").value("Feriado municipal"))
                .andExpect(jsonPath("$.reservasNaData[0]").value("14:00 às 15:00 - Reserva - Maria"));
    }

    @Test
    @DisplayName("POST /dias-especiais com horário próprio não consulta a agenda")
    void deveCadastrarHorarioProprioSemConsultarAAgenda() throws Exception {
        when(dataEspecialRepository.findByEmpresaIdAndData(eq(7L), any())).thenReturn(Optional.empty());
        when(dataEspecialRepository.save(any(DataEspecial.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post(BASE + "/dias-especiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + hoje(5) + "\",\"fechado\":false,\"abre\":\"10:00\",\"fecha\":\"16:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abre").value("10:00"))
                .andExpect(jsonPath("$.reservasNaData").isEmpty());
        verify(servicoGoogleagenda, never()).resumosDoDia(anyString(), any());
    }

    @Test
    @DisplayName("POST /dias-especiais recusa data passada")
    void deveRecusarDataPassada() throws Exception {
        mockMvc.perform(post(BASE + "/dias-especiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + hoje(-1) + "\",\"fechado\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /dias-especiais recusa data além de 180 dias")
    void deveRecusarDataAlemDoLimite() throws Exception {
        mockMvc.perform(post(BASE + "/dias-especiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + hoje(181) + "\",\"fechado\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /dias-especiais recusa data que já tem dia especial")
    void deveRecusarDataRepetida() throws Exception {
        when(dataEspecialRepository.findByEmpresaIdAndData(eq(7L), any())).thenReturn(Optional.of(new DataEspecial()));

        mockMvc.perform(post(BASE + "/dias-especiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + hoje(3) + "\",\"fechado\":true}"))
                .andExpect(status().isBadRequest());
        verify(dataEspecialRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /dias-especiais sem horário próprio completo é recusado")
    void deveExigirAberturaEFechamentoQuandoNaoFecha() throws Exception {
        when(dataEspecialRepository.findByEmpresaIdAndData(eq(7L), any())).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE + "/dias-especiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + hoje(3) + "\",\"fechado\":false,\"abre\":\"10:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /dias-especiais/{id} remove só dia especial da própria empresa")
    void deveRemoverDiaEspecial() throws Exception {
        DataEspecial especial = new DataEspecial();
        when(dataEspecialRepository.findByIdAndEmpresaId(5L, 7L)).thenReturn(Optional.of(especial));

        mockMvc.perform(delete(BASE + "/dias-especiais/5")).andExpect(status().isNoContent());
        verify(dataEspecialRepository).delete(especial);
    }

    @Test
    @DisplayName("DELETE /dias-especiais/{id} de outra empresa responde 404")
    void deveResponder404AoRemoverDiaEspecialAlheio() throws Exception {
        when(dataEspecialRepository.findByIdAndEmpresaId(anyLong(), eq(7L))).thenReturn(Optional.empty());

        mockMvc.perform(delete(BASE + "/dias-especiais/99")).andExpect(status().isNotFound());
        verify(dataEspecialRepository, never()).delete(any());
    }
}
