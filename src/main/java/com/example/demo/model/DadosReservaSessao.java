package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DadosReservaSessao {

    public static final String HORARIO_DIARIA = "Diária Completa";
    public static final int MAXIMO_DATAS = 10;

    private LocalDate data;
    private String horario;
    private String nomeCliente;
    private final List<LocalDate> datas = new ArrayList<>();

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

    public List<LocalDate> getDatas() {
        return List.copyOf(datas);
    }

    public boolean adicionarData(LocalDate nova) {
        if (nova == null || datas.contains(nova)) {
            return false;
        }
        datas.add(nova);
        datas.sort(LocalDate::compareTo);
        data = datas.get(0);
        return true;
    }

    public boolean removerData(LocalDate alvo) {
        boolean removeu = datas.remove(alvo);
        data = datas.isEmpty() ? null : datas.get(0);
        return removeu;
    }

    public boolean atingiuLimiteDeDatas() {
        return datas.size() >= MAXIMO_DATAS;
    }
}
