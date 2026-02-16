package com.example.plataformaSaaS.service;

import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import com.example.plataformaSaaS.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "billing.scheduling.enabled", havingValue = "true", matchIfMissing = false)
public class ScheduledBillingService {

    private final SuscripcionRepository suscripcionRepository;
    private final FacturaService facturaService;

    private static final int DIAS_CICLO = 30;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void procesarFacturacion() {
        log.info("Iniciando proceso de facturación automática...");
        int facturasGeneradas = ejecutarFacturacionManual();
        log.info("Proceso de facturación completado. Facturas generadas: {}", facturasGeneradas);
    }

    @Transactional
    public int ejecutarFacturacionManual() {
        LocalDate hoy = LocalDate.now();
        List<Suscripcion> suscripcionesVencidas = suscripcionRepository
                .findByEstadoAndFechaProximaFacturacionLessThanEqual(EstadoSuscripcion.ACTIVA, hoy);

        int facturasGeneradas = 0;

        for (Suscripcion suscripcion : suscripcionesVencidas) {
            try {
                facturaService.generarFacturaRenovacion(suscripcion);
                suscripcion.setFechaProximaFacturacion(hoy.plusDays(DIAS_CICLO));
                suscripcionRepository.save(suscripcion);
                facturasGeneradas++;
                log.info("Factura generada para suscripción ID: {} del usuario: {}",
                        suscripcion.getId(), suscripcion.getUsuario().getEmail());
            } catch (Exception e) {
                log.error("Error al facturar suscripción ID: {}: {}",
                        suscripcion.getId(), e.getMessage());
            }
        }

        return facturasGeneradas;
    }
}
