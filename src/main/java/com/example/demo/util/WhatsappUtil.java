package com.example.demo.util;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class WhatsappUtil {

    public static String extrairApenasNumeros(String jid) {
        if (jid == null) {
            return "";
        }
        int arroba = jid.indexOf('@');
        String base = arroba >= 0 ? jid.substring(0, arroba) : jid;
        return base.replaceAll("\\D", "");
    }

    public static String normalizarParaChaveEstado(String jid) {
        String numeros = extrairApenasNumeros(jid);
        if (!isLid(jid) && numeros.startsWith("55") && numeros.length() == 13) {
            return numeros.substring(0, 4) + numeros.substring(5);
        }
        return numeros;
    }

    public static boolean isLid(String jid) {
        return jid != null && jid.toLowerCase(Locale.ROOT).contains("@lid");
    }
}
