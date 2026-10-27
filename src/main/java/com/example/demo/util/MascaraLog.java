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

    public static String idMensagem(String id) {
        if (id == null || id.isEmpty()) {
            return OCULTO;
        }
        String[] segmentos = id.split("_", -1);
        StringBuilder saida = new StringBuilder(id.length() + 8);
        for (int i = 0; i < segmentos.length; i++) {
            if (i > 0) {
                saida.append('_');
            }
            saida.append(segmentoDeId(segmentos[i]));
        }
        return saida.toString();
    }

    private static String segmentoDeId(String segmento) {
        int arroba = segmento.indexOf('@');
        if (arroba >= 0) {
            String parte = segmento.substring(0, arroba);
            String dominio = segmento.substring(arroba);
            if (dominio.startsWith("@lid")) {
                return (parte.length() <= 4 ? "lid:" + OCULTO : "lid:" + OCULTO + parte.substring(parte.length() - 4)) + dominio;
            }
            return numero(parte) + dominio;
        }
        if (segmento.length() >= 5 && apenasDigitos(segmento)) {
            return OCULTO + segmento.substring(segmento.length() - 4);
        }
        return segmento;
    }

    private static boolean apenasDigitos(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
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
