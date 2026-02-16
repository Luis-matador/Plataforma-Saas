package com.example.plataformaSaaS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaPagoDTO {
    private Long id;
    private String concepto;
    private BigDecimal monto;
    private BigDecimal impuesto;
    private BigDecimal montoTotal;
    private BigDecimal tasaImpuesto;
    private LocalDate fechaVencimiento;
}
