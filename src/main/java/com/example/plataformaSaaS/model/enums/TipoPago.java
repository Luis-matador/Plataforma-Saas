package com.example.plataformaSaaS.model.enums;

public enum TipoPago {
    TARJETA("Tarjeta de crédito/débito"),
    PAYPAL("PayPal"),
    TRANSFERENCIA("Transferencia bancaria");

    private final String descripcion;

    TipoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
