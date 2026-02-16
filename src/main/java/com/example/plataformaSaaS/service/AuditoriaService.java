package com.example.plataformaSaaS.service;

import com.example.plataformaSaaS.dto.AuditoriaDTO;
import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.repository.PlanRepository;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditoriaService {

    private final EntityManager entityManager;
    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;

    @SuppressWarnings("unchecked")
    public List<AuditoriaDTO> obtenerAuditoriaSuscripciones() {
        List<AuditoriaDTO> auditoria = new ArrayList<>();

        try {
            var auditReader = AuditReaderFactory.get(entityManager);

            List<Object[]> results = auditReader.createQuery()
                    .forRevisionsOfEntity(Suscripcion.class, false, true)
                    .addOrder(AuditEntity.revisionNumber().desc())
                    .getResultList();

            for (Object[] result : results) {
                Suscripcion suscripcion = (Suscripcion) result[0];
                DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) result[1];
                RevisionType revisionType = (RevisionType) result[2];

                AuditoriaDTO dto = AuditoriaDTO.builder()
                        .entidadId(suscripcion.getId())
                        .tipoEntidad("Suscripción")
                        .tipoCambio(traducirRevisionType(revisionType))
                        .fechaCambio(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(revisionEntity.getTimestamp()),
                                ZoneId.systemDefault()))
                        .numeroRevision(revisionEntity.getId())
                        .build();

                try {
                    if (suscripcion.getUsuario() != null && suscripcion.getUsuario().getId() != null) {
                        usuarioRepository.findById(suscripcion.getUsuario().getId())
                                .ifPresent(u -> dto.setUsuario(u.getNombre() + " (" + u.getEmail() + ")"));
                    }
                } catch (Exception e) {
                    dto.setUsuario("N/A");
                }

                try {
                    if (suscripcion.getPlan() != null && suscripcion.getPlan().getId() != null) {
                        planRepository.findById(suscripcion.getPlan().getId())
                                .ifPresent(p -> dto.setPlanActual(p.getNombre()));
                    }
                } catch (Exception e) {
                    dto.setPlanActual("N/A");
                }

                if (suscripcion.getEstado() != null) {
                    dto.setEstadoActual(suscripcion.getEstado().getDescripcion());
                }

                auditoria.add(dto);
            }
        } catch (Exception e) {
            log.error("Error al consultar auditoría: {}", e.getMessage());
        }

        return auditoria;
    }

    private String traducirRevisionType(RevisionType type) {
        return switch (type) {
            case ADD -> "CREACIÓN";
            case MOD -> "MODIFICACIÓN";
            case DEL -> "ELIMINACIÓN";
        };
    }
}
