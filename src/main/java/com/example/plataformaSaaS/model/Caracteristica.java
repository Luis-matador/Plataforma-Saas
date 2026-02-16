package com.example.plataformaSaaS.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caracteristicas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Caracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private String icono;

    @OneToMany(mappedBy = "caracteristica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlanCaracteristica> planCaracteristicas = new ArrayList<>();
}
