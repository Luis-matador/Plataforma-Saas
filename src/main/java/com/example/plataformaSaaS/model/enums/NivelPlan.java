package com.example.plataformaSaaS.model.enums;

public enum NivelPlan {
    BASIC("Basic", 1),
    PREMIUM("Premium", 2),
    ENTERPRISE("Enterprise", 3);

    private final String nombre;
    private final int orden;

    NivelPlan(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrden() {
        return orden;
    }

    public boolean esSuperiorA(NivelPlan otro) {
        return this.orden > otro.orden;
    }
}
