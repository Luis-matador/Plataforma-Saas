package com.example.plataformaSaaS.service;

import com.example.plataformaSaaS.model.Plan;
import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.Usuario;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import com.example.plataformaSaaS.repository.PlanRepository;
import com.example.plataformaSaaS.repository.SuscripcionRepository;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final FacturaService facturaService;
    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;

    private static final int DIAS_CICLO = 30;

    public Suscripcion crearSuscripcion(Usuario usuario, Plan plan) {
        Optional<Suscripcion> existente = suscripcionRepository
                .findByUsuarioIdAndEstado(usuario.getId(), EstadoSuscripcion.ACTIVA);
        if (existente.isPresent()) {
            throw new IllegalStateException("El usuario ya tiene una suscripción activa");
        }

        Suscripcion suscripcion = Suscripcion.builder()
                .usuario(usuario)
                .plan(plan)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now())
                .fechaProximaFacturacion(LocalDate.now().plusDays(DIAS_CICLO))
                .build();

        suscripcion = suscripcionRepository.save(suscripcion);

        facturaService.generarFacturaRenovacion(suscripcion);

        return suscripcion;
    }

    public Suscripcion cambiarPlan(Long suscripcionId, Plan nuevoPlan) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Suscripción no encontrada"));

        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            throw new IllegalStateException("Solo se puede cambiar el plan de una suscripción activa");
        }

        Plan planActual = suscripcion.getPlan();

        if (planActual.getId().equals(nuevoPlan.getId())) {
            throw new IllegalArgumentException("El nuevo plan es el mismo que el actual");
        }

        boolean teniaPendientes = facturaService.tieneFacturasPendientes(suscripcion.getUsuario().getId());
        if (teniaPendientes) {
            facturaService.anularFacturasPendientes(suscripcion.getUsuario().getId());
        }

        if (nuevoPlan.getPrecioMensual().compareTo(planActual.getPrecioMensual()) > 0 && !teniaPendientes) {
            BigDecimal prorrateo = calcularProrrateo(suscripcion, nuevoPlan);
            facturaService.generarFacturaProrrateo(
                    suscripcion, prorrateo,
                    planActual.getNombre(), nuevoPlan.getNombre());
        }

        suscripcion.setPlan(nuevoPlan);
        suscripcion = suscripcionRepository.save(suscripcion);

        if (teniaPendientes) {
            facturaService.generarFacturaRenovacion(suscripcion);
        }

        return suscripcion;
    }

    public BigDecimal calcularProrrateo(Suscripcion suscripcion, Plan nuevoPlan) {
        long diasRestantes = ChronoUnit.DAYS.between(
                LocalDate.now(), suscripcion.getFechaProximaFacturacion());

        if (diasRestantes <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal precioActualDiario = suscripcion.getPlan().getPrecioMensual()
                .divide(new BigDecimal(DIAS_CICLO), 4, RoundingMode.HALF_UP);
        BigDecimal precioNuevoDiario = nuevoPlan.getPrecioMensual()
                .divide(new BigDecimal(DIAS_CICLO), 4, RoundingMode.HALF_UP);

        BigDecimal diferenciaDiaria = precioNuevoDiario.subtract(precioActualDiario);

        return diferenciaDiaria.multiply(new BigDecimal(diasRestantes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Suscripcion cancelarSuscripcion(Long suscripcionId) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Suscripción no encontrada"));

        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        suscripcion.setFechaCancelacion(LocalDate.now());
        suscripcion.setFechaFin(LocalDate.now());

        return suscripcionRepository.save(suscripcion);
    }

    @Transactional(readOnly = true)
    public Optional<Suscripcion> obtenerSuscripcionActiva(Long usuarioId) {
        return suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
    }

    public Suscripcion marcarMorosa(Long suscripcionId) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Suscripción no encontrada"));
        suscripcion.setEstado(EstadoSuscripcion.MOROSA);
        return suscripcionRepository.save(suscripcion);
    }

    public static int getDiasCiclo() {
        return DIAS_CICLO;
    }

    public Suscripcion crearSuscripcionPorIds(Long usuarioId, Long planId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
        return crearSuscripcion(usuario, plan);
    }

    public Suscripcion cambiarPlanPorUsuario(Long usuarioId, Long nuevoPlanId) {
        Suscripcion suscActiva = suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA)
                .orElseThrow(() -> new IllegalStateException("No tienes una suscripción activa"));
        Plan nuevoPlan = planRepository.findById(nuevoPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
        return cambiarPlan(suscActiva.getId(), nuevoPlan);
    }

    public Suscripcion cancelarSuscripcionPorUsuario(Long usuarioId) {
        Suscripcion suscActiva = suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA)
                .orElseThrow(() -> new IllegalStateException("No tienes una suscripción activa"));
        return cancelarSuscripcion(suscActiva.getId());
    }
}
