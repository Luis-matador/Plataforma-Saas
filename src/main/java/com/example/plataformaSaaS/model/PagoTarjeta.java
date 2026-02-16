package com.example.plataformaSaaS.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("TARJETA")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PagoTarjeta extends MetodoPago {

    @Column(name = "numero_tarjeta")
    private String numeroTarjeta;

    private String titular;

    @Column(name = "fecha_expiracion")
    private String fechaExpiracion;
}
