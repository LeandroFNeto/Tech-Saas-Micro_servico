package com.example.demo.evento;

import com.example.demo.model.Empresa;

public class EventoReservaConfirmada {

    private final Empresa empresa;
    private final String dataReserva;
    private final String horario;
    private final String nomeCliente;
    private final String numeroWhatsapp;
    private final String idPedido;

    public EventoReservaConfirmada(Empresa empresa, String dataReserva, String horario, String nomeCliente,
                                   String numeroWhatsapp, String idPedido) {
        this.empresa = empresa;
        this.dataReserva = dataReserva;
        this.horario = horario;
        this.nomeCliente = nomeCliente;
        this.numeroWhatsapp = numeroWhatsapp;
        this.idPedido = idPedido;
    }

    public Empresa getEmpresa() { return empresa; }
    public String getDataReserva() { return dataReserva; }
    public String getHorario() { return horario; }
    public String getNomeCliente() { return nomeCliente; }
    public String getNumeroWhatsapp() { return numeroWhatsapp; }
    public String getIdPedido() { return idPedido; }
}
