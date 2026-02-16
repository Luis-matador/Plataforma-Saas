package com.example.plataformaSaaS.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String telefono;

    private String direccion;

    private String ciudad;

    private String codigoPostal;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
}
