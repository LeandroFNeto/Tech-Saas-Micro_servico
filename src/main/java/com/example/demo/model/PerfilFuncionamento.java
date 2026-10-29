package com.example.demo.model;

import java.time.DayOfWeek;

public enum PerfilFuncionamento {
    SEGUNDA,
    TERCA,
    QUARTA,
    QUINTA,
    SEXTA,
    SABADO,
    DOMINGO,
    FERIADO;

    public static PerfilFuncionamento doDiaDaSemana(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> SEGUNDA;
            case TUESDAY -> TERCA;
            case WEDNESDAY -> QUARTA;
            case THURSDAY -> QUINTA;
            case FRIDAY -> SEXTA;
            case SATURDAY -> SABADO;
            case SUNDAY -> DOMINGO;
        };
    }
}
