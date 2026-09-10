package com.example.demo.servico;

import com.example.demo.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    private static final long EXPIRACAO_HORAS = 8;
    private final SecretKey chave;

    public TokenService(@Value("${JWT_SECRET_KEY:SuaChaveSuperSecretaParaGerarOsTokensJWTDe32Caracteres}") String jwtSecret) {
        this.chave = Keys.hmacShaKeyFor(normalizarChave(jwtSecret));
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("role", usuario.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(EXPIRACAO_HORAS, ChronoUnit.HOURS)))
                .signWith(chave);

        if (usuario.getEmpresa() != null) {
            builder.claim("nome", usuario.getEmpresa().getNome());
            builder.claim("sessaoWhatsapp", usuario.getEmpresa().getSessaoWhatsapp());
        } else {
            builder.claim("nome", "Administrador");
        }

        return builder.compact();
    }

    public String validarToken(String token) {
        return extrairClaims(token).getSubject();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private byte[] normalizarChave(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível para derivar JWT_SECRET_KEY", e);
        }
    }
}
