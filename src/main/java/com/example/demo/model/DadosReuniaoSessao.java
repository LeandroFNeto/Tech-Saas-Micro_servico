package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class DadosReuniaoSessao {

    private MotivoReuniao motivo;
    private LocalDate data;
    private LocalTime horario;

    public MotivoReuniao getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoReuniao motivo) {
        this.motivo = motivo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }
}
