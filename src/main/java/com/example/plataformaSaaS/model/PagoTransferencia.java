package com.example.plataformaSaaS.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("TRANSFERENCIA")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PagoTransferencia extends MetodoPago {

    @Column(name = "numero_cuenta")
    private String numeroCuenta;

    private String banco;
}
