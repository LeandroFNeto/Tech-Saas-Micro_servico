package com.example.demo.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TratadorDeErrosTest.RotasDeErro.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TratadorDeErros.class, TratadorDeErrosTest.RotasDeErro.class})
class TratadorDeErrosTest {

    record CorpoDeTeste(@NotBlank String nome) {
    }

    @RestController
    static class RotasDeErro {

        @GetMapping("/teste/interno")
        String interno() {
            throw new IllegalStateException("segredo interno: jdbc:postgresql://db/producao");
        }

        @GetMapping("/teste/sistema")
        String sistema() {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sessão inválida");
        }

        @PostMapping("/teste/validar")
        String validar(@Valid @RequestBody CorpoDeTeste corpo) {
            return corpo.nome();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Exceção não tratada vira 500 com Erro interno e sem a mensagem da exceção")
    void excecaoInternaNaoVaza() throws Exception {
        mockMvc.perform(get("/teste/interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno"))
                .andExpect(content().string(not(containsString("jdbc"))));
    }

    @Test
    @DisplayName("Mensagem do próprio sistema continua em message")
    void mensagemDoSistemaChegaEmMessage() throws Exception {
        mockMvc.perform(get("/teste/sistema"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sessão inválida"));
    }

    @Test
    @DisplayName("Erro de validação responde 400 com message")
    void validacaoResponde400() throws Exception {
        mockMvc.perform(post("/teste/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
