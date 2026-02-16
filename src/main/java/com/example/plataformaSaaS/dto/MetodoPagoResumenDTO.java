package com.example.plataformaSaaS.dto;

import com.example.plataformaSaaS.model.enums.TipoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetodoPagoResumenDTO {
    
    private Long id;
    private TipoPago tipoPago;
    private String numeroTarjeta;
    private String titular;
    private String fechaExpiracion;
    private String emailPaypal;
    private String numeroCuenta;
    private String banco;
}
