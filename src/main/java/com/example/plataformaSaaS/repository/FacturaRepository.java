package com.example.plataformaSaaS.repository;

import com.example.plataformaSaaS.model.Factura;
import com.example.plataformaSaaS.model.enums.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByUsuarioIdOrderByFechaEmisionDesc(Long usuarioId);

    List<Factura> findByFechaEmisionBetweenOrderByFechaEmisionDesc(LocalDate desde, LocalDate hasta);

    List<Factura> findByMontoTotalBetweenOrderByMontoTotalDesc(BigDecimal min, BigDecimal max);

    List<Factura> findAllByOrderByFechaEmisionDesc();

    List<Factura> findBySuscripcionId(Long suscripcionId);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoFactura estado);

    List<Factura> findByUsuarioIdAndEstado(Long usuarioId, EstadoFactura estado);

    Optional<Factura> findFirstByUsuarioIdAndEstadoOrderByFechaEmisionDesc(Long usuarioId, EstadoFactura estado);
}
