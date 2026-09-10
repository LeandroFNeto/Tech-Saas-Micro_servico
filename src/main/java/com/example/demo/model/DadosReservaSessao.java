package com.example.demo.model;

import java.time.LocalDate;

/** Rascunho da reserva em andamento, guardado em memória por conversa. */
public class DadosReservaSessao {

    public static final String HORARIO_DIARIA = "Diária Completa";

    private LocalDate data;
    private String horario;
    private String nomeCliente;

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }
}
