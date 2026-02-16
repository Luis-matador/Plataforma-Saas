package com.example.plataformaSaaS.model;

import com.example.plataformaSaaS.model.enums.TipoPago;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "metodos_pago")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_pago_class", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public abstract class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPago tipoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private boolean activo = true;
}
