package com.example.plataformaSaaS.model.enums;

public enum EstadoFactura {
    PENDIENTE("Pendiente"),
    PAGADA("Pagada"),
    VENCIDA("Vencida"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoFactura(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
