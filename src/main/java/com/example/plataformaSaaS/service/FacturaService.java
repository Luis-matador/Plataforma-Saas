package com.example.plataformaSaaS.service;

import com.example.plataformaSaaS.dto.FacturaPagoDTO;
import com.example.plataformaSaaS.model.Factura;
import com.example.plataformaSaaS.model.MetodoPago;
import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.enums.EstadoFactura;
import com.example.plataformaSaaS.repository.FacturaRepository;
import com.example.plataformaSaaS.repository.MetodoPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final MetodoPagoRepository metodoPagoRepository;

    private static final Map<String, BigDecimal> TASAS_IMPUESTO;

    static {
        Map<String, BigDecimal> tasas = new HashMap<>();
        tasas.put("España", new BigDecimal("21.00"));
        tasas.put("Mexico", new BigDecimal("16.00"));
        tasas.put("México", new BigDecimal("16.00"));
        tasas.put("Argentina", new BigDecimal("21.00"));
        tasas.put("Colombia", new BigDecimal("19.00"));
        tasas.put("Chile", new BigDecimal("19.00"));
        tasas.put("Peru", new BigDecimal("18.00"));
        tasas.put("Perú", new BigDecimal("18.00"));
        tasas.put("Estados Unidos", new BigDecimal("0.00"));
        tasas.put("USA", new BigDecimal("0.00"));
        tasas.put("Reino Unido", new BigDecimal("20.00"));
        tasas.put("Alemania", new BigDecimal("19.00"));
        tasas.put("Francia", new BigDecimal("20.00"));
        TASAS_IMPUESTO = Collections.unmodifiableMap(tasas);
    }

    private static final BigDecimal TASA_DEFAULT = new BigDecimal("10.00");

    public BigDecimal obtenerTasaImpuesto(String pais) {
        return TASAS_IMPUESTO.getOrDefault(pais, TASA_DEFAULT);
    }

    public BigDecimal calcularImpuesto(BigDecimal monto, String pais) {
        BigDecimal tasa = obtenerTasaImpuesto(pais);
        return monto.multiply(tasa).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public Factura generarFacturaRenovacion(Suscripcion suscripcion) {
        BigDecimal monto = suscripcion.getPlan().getPrecioMensual();
        String pais = suscripcion.getUsuario().getPais();
        BigDecimal impuesto = calcularImpuesto(monto, pais);
        BigDecimal montoTotal = monto.add(impuesto);
        BigDecimal tasa = obtenerTasaImpuesto(pais);

        Factura factura = Factura.builder()
                .suscripcion(suscripcion)
                .usuario(suscripcion.getUsuario())
                .concepto("Renovación mensual - Plan " + suscripcion.getPlan().getNombre())
                .monto(monto)
                .impuesto(impuesto)
                .montoTotal(montoTotal)
                .tasaImpuesto(tasa)
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .estado(EstadoFactura.PENDIENTE)
                .esProrrateo(false)
                .paisFacturacion(pais)
                .build();

        return facturaRepository.save(factura);
    }

    public Factura generarFacturaProrrateo(Suscripcion suscripcion, BigDecimal montoProrrateo,
                                            String planAnterior, String planNuevo) {
        String pais = suscripcion.getUsuario().getPais();
        BigDecimal impuesto = calcularImpuesto(montoProrrateo, pais);
        BigDecimal montoTotal = montoProrrateo.add(impuesto);
        BigDecimal tasa = obtenerTasaImpuesto(pais);

        Factura factura = Factura.builder()
                .suscripcion(suscripcion)
                .usuario(suscripcion.getUsuario())
                .concepto("Prorrateo por cambio de plan: " + planAnterior + " → " + planNuevo)
                .monto(montoProrrateo)
                .impuesto(impuesto)
                .montoTotal(montoTotal)
                .tasaImpuesto(tasa)
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .estado(EstadoFactura.PENDIENTE)
                .esProrrateo(true)
                .paisFacturacion(pais)
                .build();

        return facturaRepository.save(factura);
    }


    @Transactional(readOnly = true)
    public Optional<Factura> buscarPorId(Long id) {
        return facturaRepository.findById(id);
    }

    public Factura pagarFactura(Long facturaId, MetodoPago metodoPago) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        if (factura.getEstado() != EstadoFactura.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden pagar facturas pendientes");
        }

        factura.setEstado(EstadoFactura.PAGADA);
        factura.setFechaPago(LocalDate.now());
        factura.setMetodoPago(metodoPago);
        return facturaRepository.save(factura);
    }

    @Transactional(readOnly = true)
    public List<Factura> listarFacturasUsuario(Long usuarioId) {
        return facturaRepository.findByUsuarioIdOrderByFechaEmisionDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Factura> filtrarPorFecha(LocalDate desde, LocalDate hasta) {
        return facturaRepository.findByFechaEmisionBetweenOrderByFechaEmisionDesc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<Factura> filtrarPorMonto(BigDecimal min, BigDecimal max) {
        return facturaRepository.findByMontoTotalBetweenOrderByMontoTotalDesc(min, max);
    }

    @Transactional(readOnly = true)
    public List<Factura> listarTodas() {
        return facturaRepository.findAllByOrderByFechaEmisionDesc();
    }

    public static Map<String, BigDecimal> getTasasImpuesto() {
        return TASAS_IMPUESTO;
    }

    @Transactional(readOnly = true)
    public boolean tieneFacturasPendientes(Long usuarioId) {
        return facturaRepository.existsByUsuarioIdAndEstado(usuarioId, EstadoFactura.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public Long obtenerUltimaFacturaPendienteId(Long usuarioId) {
        return facturaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaEmisionDesc(usuarioId, EstadoFactura.PENDIENTE)
                .map(Factura::getId)
                .orElse(null);
    }

    public void anularFacturasPendientes(Long usuarioId) {
        List<Factura> pendientes = facturaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoFactura.PENDIENTE);
        for (Factura f : pendientes) {
            f.setEstado(EstadoFactura.ANULADA);
        }
        facturaRepository.saveAll(pendientes);
    }

    @Transactional(readOnly = true)
    public FacturaPagoDTO obtenerFacturaParaPago(Long facturaId, Long usuarioId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        if (!factura.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Factura no encontrada");
        }

        if (!factura.getEstado().name().equals("PENDIENTE")) {
            throw new IllegalStateException("Esta factura no se puede pagar");
        }

        return FacturaPagoDTO.builder()
                .id(factura.getId())
                .concepto(factura.getConcepto())
                .monto(factura.getMonto())
                .impuesto(factura.getImpuesto())
                .montoTotal(factura.getMontoTotal())
                .tasaImpuesto(factura.getTasaImpuesto())
                .fechaVencimiento(factura.getFechaVencimiento())
                .build();
    }

    public Factura procesarPago(Long facturaId, Long metodoPagoId, Long usuarioId) {
        MetodoPago metodo = metodoPagoRepository.findById(metodoPagoId)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado"));

        if (!metodo.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Metodo de pago no valido");
        }

        return pagarFactura(facturaId, metodo);
    }

    @Transactional(readOnly = true)
    public List<Factura> filtrarFacturasUsuarioPorFecha(Long usuarioId, LocalDate desde, LocalDate hasta) {
        return facturaRepository.findByUsuarioIdOrderByFechaEmisionDesc(usuarioId).stream()
                .filter(f -> !f.getFechaEmision().isBefore(desde) && !f.getFechaEmision().isAfter(hasta))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Factura> filtrarFacturasUsuarioPorMonto(Long usuarioId, BigDecimal montoMin, BigDecimal montoMax) {
        return facturaRepository.findByUsuarioIdOrderByFechaEmisionDesc(usuarioId).stream()
                .filter(f -> f.getMontoTotal().compareTo(montoMin) >= 0
                        && f.getMontoTotal().compareTo(montoMax) <= 0)
                .toList();
    }
}
