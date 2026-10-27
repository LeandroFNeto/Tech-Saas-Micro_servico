package com.example.demo.util;

public final class MascaraLog {

    private static final String OCULTO = "***";

    private MascaraLog() {
    }

    public static String numero(String jid) {
        if (jid == null) {
            return OCULTO;
        }
        int arroba = jid.indexOf('@');
        String parte = arroba >= 0 ? jid.substring(0, arroba) : jid;
        String digitos = parte.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return OCULTO;
        }
        return OCULTO + digitos.substring(digitos.length() - 4);
    }

    public static String jid(String jid) {
        if (jid == null) {
            return OCULTO;
        }
        if (jid.contains("@lid")) {
            String parte = jid.substring(0, jid.indexOf('@'));
            if (parte.length() <= 4) {
                return "lid:" + OCULTO;
            }
            return "lid:" + OCULTO + parte.substring(parte.length() - 4);
        }
        return numero(jid);
    }

    public static String email(String email) {
        if (email == null) {
            return OCULTO;
        }
        int arroba = email.indexOf('@');
        if (arroba < 1) {
            return OCULTO;
        }
        return email.charAt(0) + OCULTO + email.substring(arroba);
    }

    public static String tamanho(String texto) {
        int n = texto == null ? 0 : texto.length();
        return n + " caracteres";
    }
}
