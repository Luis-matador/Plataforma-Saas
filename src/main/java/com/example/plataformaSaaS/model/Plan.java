package com.example.plataformaSaaS.model;

import com.example.plataformaSaaS.model.enums.NivelPlan;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private NivelPlan nivel;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlanCaracteristica> planCaracteristicas = new ArrayList<>();
}
