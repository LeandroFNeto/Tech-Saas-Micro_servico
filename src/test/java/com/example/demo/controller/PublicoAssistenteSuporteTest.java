package com.example.demo.controller;

import com.example.demo.dto.assistente.AssistenteChatResponseDTO;
import com.example.demo.servico.LimitadorPorIp;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicoAssistenteSuporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(LimitadorPorIp.class)
class PublicoAssistenteSuporteTest {

    private static final String ROTA = "/api/publico/assistente/suporte";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistenteTutoriaService assistenteTutoriaService;

    private RequestPostProcessor doIp(String ip) {
        return requisicao -> {
            requisicao.setRemoteAddr(ip);
            return requisicao;
        };
    }

    private String corpo(String mensagem) {
        return "{\"mensagem\":\"" + mensagem + "\"}";
    }

    @Test
    @DisplayName("POST /api/publico/assistente/suporte sem login responde 200 com o bot de Suporte")
    void deveResponder200SemLogin() throws Exception {
        when(assistenteTutoriaService.responderSuporte(anyString()))
                .thenReturn(new AssistenteChatResponseDTO("Acesse a Conexão WhatsApp.", "SUPORTE", Instant.now(), List.of()));

        mockMvc.perform(post(ROTA).with(doIp("10.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Como conecto o WhatsApp?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("SUPORTE"));
    }

    @Test
    @DisplayName("Mensagem vazia ou acima de 500 caracteres responde 400")
    void deveResponder400ParaMensagemInvalida() throws Exception {
        mockMvc.perform(post(ROTA).with(doIp("10.0.0.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("   ")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(ROTA).with(doIp("10.0.0.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("a".repeat(501))))
                .andExpect(status().isBadRequest());

        verify(assistenteTutoriaService, never()).responderSuporte(anyString());
    }

    @Test
    @DisplayName("Acima de 10 mensagens por minuto no mesmo IP responde 429 sem chamar o modelo")
    void deveResponder429AcimaDoLimite() throws Exception {
        when(assistenteTutoriaService.responderSuporte(anyString()))
                .thenReturn(new AssistenteChatResponseDTO("ok", "SUPORTE", Instant.now(), List.of()));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(ROTA).with(doIp("10.0.0.3"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("duvida")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post(ROTA).with(doIp("10.0.0.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("duvida")))
                .andExpect(status().isTooManyRequests());

        verify(assistenteTutoriaService, times(10)).responderSuporte(anyString());
    }
}
