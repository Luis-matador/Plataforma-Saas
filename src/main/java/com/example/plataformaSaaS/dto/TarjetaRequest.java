package com.example.plataformaSaaS.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TarjetaRequest {

    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Size(min = 13, max = 19, message = "Número de tarjeta inválido")
    private String numeroTarjeta;

    @NotBlank(message = "El nombre del titular es obligatorio")
    private String titular;

    @NotBlank(message = "La fecha de expiración es obligatoria")
    @Size(min = 5, max = 5, message = "Formato: MM/YY")
    private String fechaExpiracion;
}
