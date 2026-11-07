package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    @Test
    @DisplayName("Gera e valida token JWT de CLIENTE com claims de empresa usando segredo de 32+ bytes")
    void deveGerarEValidarTokenDeCliente() {
        TokenService service = new TokenService("ChaveDeTesteSeguraComMaisDeTrintaEDoisCaracteres123");
        Empresa empresa = new Empresa();
        empresa.setNome("Recanto Sol");
        empresa.setSessaoWhatsapp("recanto_sol");

        Usuario usuario = new Usuario();
        usuario.setEmail("recanto_sol@gamb.com.br");
        usuario.setRole(Role.CLIENTE);
        usuario.setEmpresa(empresa);

        String token = service.gerarToken(usuario);

        assertEquals("recanto_sol@gamb.com.br", service.validarToken(token));
        Claims claims = service.extrairClaims(token);
        assertEquals("CLIENTE", claims.get("role", String.class));
        assertEquals("Recanto Sol", claims.get("nome", String.class));
        assertEquals("recanto_sol", claims.get("sessaoWhatsapp", String.class));
    }

    @Test
    @DisplayName("Deriva chave via SHA-256 quando segredo tem menos de 32 bytes e preenche Administrador sem empresa")
    void deveDerivarChaveCurtaEGerarTokenDeAdmin() {
        TokenService service = new TokenService("segredo-curto");
        Usuario admin = new Usuario();
        admin.setEmail("admin@gamb.com.br");
        admin.setRole(Role.ADMIN);

        String token = service.gerarToken(admin);
        Claims claims = service.extrairClaims(token);

        assertEquals("admin@gamb.com.br", service.validarToken(token));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("Administrador", claims.get("nome", String.class));
        assertNull(claims.get("sessaoWhatsapp"));
    }

    @Test
    @DisplayName("Rejeita token adulterado ou assinado com outra chave")
    void deveRejeitarTokenInvalidoOuDeOutraChave() {
        TokenService emissor = new TokenService("ChaveEmissoraComMaisDeTrintaEDoisBytesValida!");
        TokenService outro = new TokenService("OutraChaveDiferenteComMaisDeTrintaEDoisBytes!");

        Usuario admin = new Usuario();
        admin.setEmail("admin@gamb.com.br");
        admin.setRole(Role.ADMIN);
        String token = emissor.gerarToken(admin);

        assertThrows(JwtException.class, () -> outro.validarToken(token));
        assertThrows(JwtException.class, () -> emissor.validarToken(token + "adulterado"));
    }
}
