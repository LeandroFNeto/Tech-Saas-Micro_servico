package com.example.demo.model;

public enum MotivoReuniao {
    AJUSTE("Ajuste"),
    NOVA_INOVACAO("Nova inovação");

    private final String rotulo;

    MotivoReuniao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
