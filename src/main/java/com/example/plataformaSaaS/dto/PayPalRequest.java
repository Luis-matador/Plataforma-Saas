package com.example.plataformaSaaS.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PayPalRequest {

    @NotBlank(message = "El email de PayPal es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String emailPaypal;
}
