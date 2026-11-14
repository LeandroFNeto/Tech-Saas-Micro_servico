package com.example.demo.util;

public final class LiteralDeIp {

    private LiteralDeIp() {
    }

    public static byte[] bytes(String texto) {
        if (texto == null || texto.isEmpty()) {
            return null;
        }
        if (texto.indexOf(':') >= 0) {
            return ipv6(texto);
        }
        return ipv4(texto);
    }

    private static byte[] ipv4(String texto) {
        String[] octetos = texto.split("\\.", -1);
        if (octetos.length != 4) {
            return null;
        }
        byte[] saida = new byte[4];
        for (int i = 0; i < 4; i++) {
            String octeto = octetos[i];
            if (octeto.isEmpty() || octeto.length() > 3 || (octeto.length() > 1 && octeto.charAt(0) == '0')) {
                return null;
            }
            int valor = 0;
            for (int j = 0; j < octeto.length(); j++) {
                char c = octeto.charAt(j);
                if (c < '0' || c > '9') {
                    return null;
                }
                valor = valor * 10 + (c - '0');
            }
            if (valor > 255) {
                return null;
            }
            saida[i] = (byte) valor;
        }
        return saida;
    }

    private static byte[] ipv6(String original) {
        String texto = original;
        int ultimaVirgula = texto.lastIndexOf(':');
        if (texto.indexOf('.') >= 0) {
            byte[] embutido = ipv4(texto.substring(ultimaVirgula + 1));
            if (embutido == null) {
                return null;
            }
            texto = texto.substring(0, ultimaVirgula + 1)
                    + Integer.toHexString(((embutido[0] & 0xFF) << 8) | (embutido[1] & 0xFF)) + ":"
                    + Integer.toHexString(((embutido[2] & 0xFF) << 8) | (embutido[3] & 0xFF));
        }
        int compressao = texto.indexOf("::");
        if (compressao >= 0 && texto.indexOf("::", compressao + 1) >= 0) {
            return null;
        }
        int[] grupos = new int[8];
        if (compressao < 0) {
            String[] partes = texto.split(":", -1);
            if (partes.length != 8 || !preencher(partes, grupos, 0)) {
                return null;
            }
        } else {
            String inicio = texto.substring(0, compressao);
            String fim = texto.substring(compressao + 2);
            String[] aEsquerda = inicio.isEmpty() ? new String[0] : inicio.split(":", -1);
            String[] aDireita = fim.isEmpty() ? new String[0] : fim.split(":", -1);
            if (aEsquerda.length + aDireita.length > 7) {
                return null;
            }
            if (!preencher(aEsquerda, grupos, 0) || !preencher(aDireita, grupos, 8 - aDireita.length)) {
                return null;
            }
        }
        boolean mapeadoIpv4 = grupos[0] == 0 && grupos[1] == 0 && grupos[2] == 0 && grupos[3] == 0
                && grupos[4] == 0 && grupos[5] == 0xFFFF;
        if (mapeadoIpv4) {
            return new byte[]{
                    (byte) (grupos[6] >> 8), (byte) grupos[6], (byte) (grupos[7] >> 8), (byte) grupos[7]
            };
        }
        byte[] saida = new byte[16];
        for (int i = 0; i < 8; i++) {
            saida[2 * i] = (byte) (grupos[i] >> 8);
            saida[2 * i + 1] = (byte) grupos[i];
        }
        return saida;
    }

    private static boolean preencher(String[] partes, int[] grupos, int deslocamento) {
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            if (parte.isEmpty() || parte.length() > 4) {
                return false;
            }
            int valor = 0;
            for (int j = 0; j < parte.length(); j++) {
                char c = parte.charAt(j);
                boolean hexa = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
                if (!hexa) {
                    return false;
                }
                valor = valor * 16 + Character.digit(c, 16);
            }
            grupos[deslocamento + i] = valor;
        }
        return true;
    }
}
