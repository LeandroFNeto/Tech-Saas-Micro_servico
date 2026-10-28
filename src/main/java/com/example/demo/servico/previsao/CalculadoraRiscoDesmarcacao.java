package com.example.demo.servico.previsao;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculadoraRiscoDesmarcacao {

    public enum Risco { BAIXO, MEDIO, ALTO }

    private CalculadoraRiscoDesmarcacao() {
    }

    public static int arredondarChance(double probabilidade) {
        return (int) Math.round(probabilidade * 100);
    }

    public static double arredondarChuva(double chuvaMm) {
        return BigDecimal.valueOf(chuvaMm).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public static boolean isTemporal(int codigoTempo) {
        return codigoTempo >= 200 && codigoTempo <= 299;
    }

    public static Risco classificar(double probabilidade, double chuvaMm, int codigoTempo) {
        int chance = arredondarChance(probabilidade);
        double chuva = arredondarChuva(chuvaMm);
        if (chance >= 70 || isTemporal(codigoTempo) || chuva >= 10.0) {
            return Risco.ALTO;
        }
        if (chance >= 40 || chuva >= 2.0) {
            return Risco.MEDIO;
        }
        return Risco.BAIXO;
    }
}
