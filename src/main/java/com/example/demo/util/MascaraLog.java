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
        int arroba = id.indexOf('@');
        if (arroba < 0) {
            return id;
        }
        int inicio = id.lastIndexOf('_', arroba) + 1;
        String parte = id.substring(inicio, arroba);
        int fimDominio = id.indexOf('_', arroba);
        String dominio = fimDominio < 0 ? id.substring(arroba) : id.substring(arroba, fimDominio);
        String resto = fimDominio < 0 ? "" : id.substring(fimDominio);
        String mascarado;
        if (dominio.startsWith("@lid")) {
            mascarado = parte.length() <= 4 ? "lid:" + OCULTO : "lid:" + OCULTO + parte.substring(parte.length() - 4);
        } else {
            mascarado = numero(parte);
        }
        return id.substring(0, inicio) + mascarado + dominio + resto;
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
