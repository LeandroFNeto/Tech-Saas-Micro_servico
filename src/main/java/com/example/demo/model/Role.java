package com.example.demo.model;

public enum Role {
    ADMIN,
    SUPER_ADMIN,
    ADMIN_OPERACIONAL,
    ADMIN_LEITURA,
    CLIENTE;

    public boolean isAdmin() {
        return this != CLIENTE;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean isOperacional() {
        return this == SUPER_ADMIN || this == ADMIN || this == ADMIN_OPERACIONAL;
    }

    public boolean isLeitura() {
        return this == ADMIN_LEITURA;
    }
}
