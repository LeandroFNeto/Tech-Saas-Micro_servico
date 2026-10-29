package com.example.demo.evento;

import com.example.demo.model.Empresa;

import java.util.List;

public class EventoReservaConfirmada {

    private final Empresa empresa;
    private final List<String> datas;
    private final String horario;
    private final String nomeCliente;
    private final String numeroWhatsapp;
    private final String idPedido;
    private final boolean porHora;

    public EventoReservaConfirmada(Empresa empresa, String dataReserva, String horario, String nomeCliente,
                                   String numeroWhatsapp, String idPedido) {
        this(empresa, List.of(dataReserva), horario, nomeCliente, numeroWhatsapp, idPedido, false);
    }

    public EventoReservaConfirmada(Empresa empresa, List<String> datas, String horario, String nomeCliente,
                                   String numeroWhatsapp, String idPedido, boolean porHora) {
        this.empresa = empresa;
        this.datas = List.copyOf(datas);
        this.horario = horario;
        this.nomeCliente = nomeCliente;
        this.numeroWhatsapp = numeroWhatsapp;
        this.idPedido = idPedido;
        this.porHora = porHora;
    }

    public Empresa getEmpresa() { return empresa; }
    public String getDataReserva() { return datas.get(0); }
    public List<String> getDatas() { return datas; }
    public String getHorario() { return horario; }
    public String getNomeCliente() { return nomeCliente; }
    public String getNumeroWhatsapp() { return numeroWhatsapp; }
    public String getIdPedido() { return idPedido; }
    public boolean isPorHora() { return porHora; }
}
