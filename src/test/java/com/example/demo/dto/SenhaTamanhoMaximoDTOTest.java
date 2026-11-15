package com.example.demo.dto;

import com.example.demo.dto.empresa.EmpresaCreateDTO;
import com.example.demo.dto.empresa.ResetSenhaAdminDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaTamanhoMaximoDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private static EmpresaCreateDTO empresaComSenha(String senha) {
        return new EmpresaCreateDTO("Recanto", "sessao_01", null, null, null, null, null, null, null, null, null, senha, null);
    }

    @Test
    @DisplayName("Redefinicao de senha com 129 caracteres e recusada e com 128 e aceita")
    void redefinicaoRespeitaMaximo() {
        assertFalse(validador.validate(new ResetSenhaAdminDTO("a".repeat(129))).isEmpty());
        assertTrue(validador.validate(new ResetSenhaAdminDTO("a".repeat(128))).isEmpty());
    }

    @Test
    @DisplayName("Criacao de empresa com senha de 129 caracteres e recusada e com 128 e aceita")
    void criacaoRespeitaMaximo() {
        assertFalse(validador.validate(empresaComSenha("a".repeat(129))).isEmpty());
        assertTrue(validador.validate(empresaComSenha("a".repeat(128))).isEmpty());
    }
}
