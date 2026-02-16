package com.example.plataformaSaaS.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditoriaDTO {

    private Long entidadId;
    private String tipoEntidad;
    private String tipoCambio;
    private LocalDateTime fechaCambio;
    private String usuario;
    private String planAnterior;
    private String planActual;
    private String estadoAnterior;
    private String estadoActual;
    private int numeroRevision;
}
