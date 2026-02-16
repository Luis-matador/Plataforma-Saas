package com.example.plataformaSaaS.repository;

import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    Optional<Suscripcion> findByUsuarioIdAndEstado(Long usuarioId, EstadoSuscripcion estado);

    List<Suscripcion> findByEstadoAndFechaProximaFacturacionLessThanEqual(
            EstadoSuscripcion estado, LocalDate fecha);

    List<Suscripcion> findByUsuarioId(Long usuarioId);
}
