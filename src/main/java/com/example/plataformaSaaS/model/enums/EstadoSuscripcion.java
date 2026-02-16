package com.example.plataformaSaaS.model.enums;

public enum EstadoSuscripcion {
    ACTIVA("Activa"),
    CANCELADA("Cancelada"),
    MOROSA("Morosa");

    private final String descripcion;

    EstadoSuscripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
