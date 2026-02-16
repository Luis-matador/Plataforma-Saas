package com.example.plataformaSaaS.model;

import com.example.plataformaSaaS.model.enums.EstadoFactura;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "facturas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal impuesto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaImpuesto;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFactura estado;

    @Column(nullable = false)
    @Builder.Default
    private boolean esProrrateo = false;

    private String paisFacturacion;

    private LocalDate fechaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;
}
