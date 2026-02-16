package com.example.plataformaSaaS.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_caracteristicas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"plan_id", "caracteristica_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlanCaracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caracteristica_id", nullable = false)
    private Caracteristica caracteristica;

    @Column(nullable = false)
    private int limite;

    @Column(nullable = false)
    private String valor;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;
}
