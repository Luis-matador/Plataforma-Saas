package com.example.plataformaSaaS.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("PAYPAL")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PagoPayPal extends MetodoPago {

    @Column(name = "email_paypal")
    private String emailPaypal;
}
