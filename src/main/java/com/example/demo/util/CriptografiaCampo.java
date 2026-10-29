package com.example.demo.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

@Converter
@Component
public class CriptografiaCampo implements AttributeConverter<String, String> {

    private static final String VERSAO_PREFIXO = "v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(CriptografiaCampo.class);
    private static final AtomicBoolean avisoEmitido = new AtomicBoolean(false);

    private static volatile String chaveCriptoBase64;
    private static volatile String chaveIndice;

    public CriptografiaCampo(
            @Value("${contatos.cripto.chave:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}") String chaveCripto,
            @Value("${contatos.indice.chave:chave-teste-hmac-indice-32-bytes!}") String chaveIndiceConfig) {
        configurarChaves(chaveCripto, chaveIndiceConfig);
    }

    public CriptografiaCampo() {
    }

    public static synchronized void configurarChaves(String chaveCripto, String chaveIndiceConfig) {
        chaveCriptoBase64 = (chaveCripto != null && !chaveCripto.isBlank()) ? chaveCripto.trim() : null;
        chaveIndice = (chaveIndiceConfig != null && !chaveIndiceConfig.isBlank()) ? chaveIndiceConfig.trim() : null;
    }

    public static boolean isConfigurado() {
        if (chaveCriptoBase64 == null || chaveIndice == null) {
            return false;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(chaveCriptoBase64);
            return bytes.length == 32 && !chaveIndice.isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void avisarChavesNaoConfiguradasSeNecessario() {
        if (!isConfigurado() && avisoEmitido.compareAndSet(false, true)) {
            log.warn("Chaves de criptografia e indice de contatos nao configuradas. Gravacao de contatos e lembretes desativados.");
        }
    }

    public static void resetarAviso() {
        avisoEmitido.set(false);
    }

    public static String criptografar(String texto) {
        if (texto == null) {
            return null;
        }
        if (!isConfigurado()) {
            avisarChavesNaoConfiguradasSeNecessario();
            throw new IllegalStateException("Criptografia de contatos nao configurada.");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(chaveCriptoBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return VERSAO_PREFIXO + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criptografar campo.", e);
        }
    }

    public static String descriptografar(String cifrado) {
        if (cifrado == null) {
            return null;
        }
        if (!cifrado.startsWith(VERSAO_PREFIXO)) {
            return cifrado;
        }
        if (!isConfigurado()) {
            avisarChavesNaoConfiguradasSeNecessario();
            throw new IllegalStateException("Criptografia de contatos nao configurada.");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(chaveCriptoBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] combined = Base64.getDecoder().decode(cifrado.substring(VERSAO_PREFIXO.length()));
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descriptografar campo.", e);
        }
    }

    public static String calcularIndice(String numeroChave) {
        if (numeroChave == null) {
            return null;
        }
        if (!isConfigurado()) {
            avisarChavesNaoConfiguradasSeNecessario();
            throw new IllegalStateException("Chave de indice nao configurada.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveIndice.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(numeroChave.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular indice HMAC.", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return criptografar(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return descriptografar(dbData);
    }
}
