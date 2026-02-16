package com.example.plataformaSaaS;

import com.example.plataformaSaaS.model.*;
import com.example.plataformaSaaS.model.enums.EstadoFactura;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import com.example.plataformaSaaS.model.enums.TipoPago;
import com.example.plataformaSaaS.repository.FacturaRepository;
import com.example.plataformaSaaS.repository.MetodoPagoRepository;
import com.example.plataformaSaaS.service.FacturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FacturaService - Tests de facturación e impuestos")
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @InjectMocks
    private FacturaService facturaService;

    private Usuario usuarioEspana;
    private Usuario usuarioMexico;
    private Usuario usuarioUSA;
    private Usuario usuarioDesconocido;
    private Plan planBasic;
    private Plan planPremium;
    private Suscripcion suscripcionEspana;
    private Suscripcion suscripcionMexico;

    @BeforeEach
    void setUp() {
        usuarioEspana = Usuario.builder()
                .id(1L).nombre("Juan").email("juan@test.com")
                .password("123").pais("España").build();

        usuarioMexico = Usuario.builder()
                .id(2L).nombre("Carlos").email("carlos@test.com")
                .password("123").pais("México").build();

        usuarioUSA = Usuario.builder()
                .id(3L).nombre("John").email("john@test.com")
                .password("123").pais("USA").build();

        usuarioDesconocido = Usuario.builder()
                .id(4L).nombre("Test").email("test@test.com")
                .password("123").pais("Japón").build();

        planBasic = Plan.builder()
                .id(1L).nombre("Basic").nivel(NivelPlan.BASIC)
                .precioMensual(new BigDecimal("9.99")).activo(true).build();

        planPremium = Plan.builder()
                .id(2L).nombre("Premium").nivel(NivelPlan.PREMIUM)
                .precioMensual(new BigDecimal("29.99")).activo(true).build();

        suscripcionEspana = Suscripcion.builder()
                .id(1L).usuario(usuarioEspana).plan(planBasic)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now())
                .fechaProximaFacturacion(LocalDate.now().plusDays(30))
                .build();

        suscripcionMexico = Suscripcion.builder()
                .id(2L).usuario(usuarioMexico).plan(planPremium)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now())
                .fechaProximaFacturacion(LocalDate.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("Impuesto España: 21% IVA")
    void testImpuestoEspana() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "España");
        assertEquals(new BigDecimal("21.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto México: 16% IVA")
    void testImpuestoMexico() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "México");
        assertEquals(new BigDecimal("16.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto Argentina: 21% IVA")
    void testImpuestoArgentina() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "Argentina");
        assertEquals(new BigDecimal("21.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto Colombia: 19% IVA")
    void testImpuestoColombia() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "Colombia");
        assertEquals(new BigDecimal("19.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto Chile: 19% IVA")
    void testImpuestoChile() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "Chile");
        assertEquals(new BigDecimal("19.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto USA: 0%")
    void testImpuestoUSA() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "USA");
        assertEquals(new BigDecimal("0.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto país desconocido: 10% por defecto")
    void testImpuestoPaisDesconocido() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "Japón");
        assertEquals(new BigDecimal("10.00"), impuesto);
    }

    @Test
    @DisplayName("Impuesto sobre monto decimal")
    void testImpuestoMontoDecimal() {
        BigDecimal monto = new BigDecimal("9.99");
        BigDecimal impuesto = facturaService.calcularImpuesto(monto, "España");
        assertEquals(new BigDecimal("2.10"), impuesto);
    }

    @Test
    @DisplayName("Generar factura de renovación con impuestos correctos (España)")
    void testGenerarFacturaRenovacionEspana() {
        when(facturaRepository.save(any(Factura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Factura factura = facturaService.generarFacturaRenovacion(suscripcionEspana);

        assertNotNull(factura);
        assertEquals(new BigDecimal("9.99"), factura.getMonto());
        assertEquals(new BigDecimal("2.10"), factura.getImpuesto());
        assertEquals(new BigDecimal("12.09"), factura.getMontoTotal());
        assertEquals(new BigDecimal("21.00"), factura.getTasaImpuesto());
        assertEquals(EstadoFactura.PENDIENTE, factura.getEstado());
        assertFalse(factura.isEsProrrateo());
        assertEquals("España", factura.getPaisFacturacion());
        assertEquals(LocalDate.now(), factura.getFechaEmision());
        assertEquals(LocalDate.now().plusDays(15), factura.getFechaVencimiento());

        verify(facturaRepository, times(1)).save(any(Factura.class));
    }

    @Test
    @DisplayName("Generar factura de renovación sin impuestos (USA)")
    void testGenerarFacturaRenovacionUSA() {
        Suscripcion suscripcionUSA = Suscripcion.builder()
                .id(3L).usuario(usuarioUSA).plan(planBasic)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now())
                .fechaProximaFacturacion(LocalDate.now().plusDays(30))
                .build();

        when(facturaRepository.save(any(Factura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Factura factura = facturaService.generarFacturaRenovacion(suscripcionUSA);

        assertEquals(new BigDecimal("9.99"), factura.getMonto());
        assertEquals(new BigDecimal("0.00"), factura.getImpuesto());
        assertEquals(new BigDecimal("9.99"), factura.getMontoTotal());
    }

    @Test
    @DisplayName("Generar factura de prorrateo")
    void testGenerarFacturaProrrateo() {
        BigDecimal montoProrrateo = new BigDecimal("10.00");

        when(facturaRepository.save(any(Factura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Factura factura = facturaService.generarFacturaProrrateo(
                suscripcionEspana, montoProrrateo, "Basic", "Premium");

        assertNotNull(factura);
        assertTrue(factura.isEsProrrateo());
        assertEquals(new BigDecimal("10.00"), factura.getMonto());
        assertEquals(new BigDecimal("2.10"), factura.getImpuesto());
        assertEquals(new BigDecimal("12.10"), factura.getMontoTotal());
        assertTrue(factura.getConcepto().contains("Prorrateo"));
        assertTrue(factura.getConcepto().contains("Basic"));
        assertTrue(factura.getConcepto().contains("Premium"));
    }

    @Test
    @DisplayName("Factura se guarda en el repositorio")
    void testFacturaSeGuardaEnRepositorio() {
        when(facturaRepository.save(any(Factura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        facturaService.generarFacturaRenovacion(suscripcionEspana);

        ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());

        Factura facturaGuardada = captor.getValue();
        assertEquals(suscripcionEspana, facturaGuardada.getSuscripcion());
        assertEquals(usuarioEspana, facturaGuardada.getUsuario());
    }

    @Test
    @DisplayName("Obtener tasa de impuesto retorna valor correcto")
    void testObtenerTasaImpuesto() {
        assertEquals(new BigDecimal("21.00"), facturaService.obtenerTasaImpuesto("España"));
        assertEquals(new BigDecimal("16.00"), facturaService.obtenerTasaImpuesto("México"));
        assertEquals(new BigDecimal("0.00"), facturaService.obtenerTasaImpuesto("USA"));
        assertEquals(new BigDecimal("10.00"), facturaService.obtenerTasaImpuesto("PaísInventado"));
    }

    @Test
    @DisplayName("Pagar factura pendiente correctamente")
    void testPagarFacturaPendiente() {
        Factura facturaPendiente = Factura.builder()
                .id(10L)
                .suscripcion(suscripcionEspana)
                .usuario(usuarioEspana)
                .concepto("Renovación mensual - Plan Basic")
                .monto(new BigDecimal("9.99"))
                .impuesto(new BigDecimal("2.10"))
                .montoTotal(new BigDecimal("12.09"))
                .tasaImpuesto(new BigDecimal("21.00"))
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .estado(EstadoFactura.PENDIENTE)
                .esProrrateo(false)
                .paisFacturacion("España")
                .build();

        PagoTarjeta metodo = new PagoTarjeta();
        metodo.setId(1L);
        metodo.setTipoPago(TipoPago.TARJETA);
        metodo.setNumeroTarjeta("4242");
        metodo.setTitular("Marco Cardo");

        when(facturaRepository.findById(10L)).thenReturn(Optional.of(facturaPendiente));
        when(facturaRepository.save(any(Factura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Factura resultado = facturaService.pagarFactura(10L, metodo);

        assertEquals(EstadoFactura.PAGADA, resultado.getEstado());
        assertEquals(LocalDate.now(), resultado.getFechaPago());
        assertEquals(metodo, resultado.getMetodoPago());
        verify(facturaRepository, times(1)).save(any(Factura.class));
    }

    @Test
    @DisplayName("No se puede pagar factura ya pagada")
    void testPagarFacturaYaPagadaFalla() {
        Factura facturaPagada = Factura.builder()
                .id(10L).estado(EstadoFactura.PAGADA).build();

        when(facturaRepository.findById(10L)).thenReturn(Optional.of(facturaPagada));

        PagoTarjeta metodo = new PagoTarjeta();
        assertThrows(IllegalStateException.class, () ->
                facturaService.pagarFactura(10L, metodo));
    }

    @Test
    @DisplayName("No se puede pagar factura anulada")
    void testPagarFacturaAnuladaFalla() {
        Factura facturaAnulada = Factura.builder()
                .id(10L).estado(EstadoFactura.ANULADA).build();

        when(facturaRepository.findById(10L)).thenReturn(Optional.of(facturaAnulada));

        PagoTarjeta metodo = new PagoTarjeta();
        assertThrows(IllegalStateException.class, () ->
                facturaService.pagarFactura(10L, metodo));
    }

    @Test
    @DisplayName("No se puede pagar factura inexistente")
    void testPagarFacturaInexistenteFalla() {
        when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

        PagoTarjeta metodo = new PagoTarjeta();
        assertThrows(IllegalArgumentException.class, () ->
                facturaService.pagarFactura(99L, metodo));
    }

    @Test
    @DisplayName("tieneFacturasPendientes retorna true cuando hay pendientes")
    void testTieneFacturasPendientesTrue() {
        when(facturaRepository.existsByUsuarioIdAndEstado(1L, EstadoFactura.PENDIENTE))
                .thenReturn(true);

        assertTrue(facturaService.tieneFacturasPendientes(1L));
    }

    @Test
    @DisplayName("tieneFacturasPendientes retorna false cuando no hay pendientes")
    void testTieneFacturasPendientesFalse() {
        when(facturaRepository.existsByUsuarioIdAndEstado(1L, EstadoFactura.PENDIENTE))
                .thenReturn(false);

        assertFalse(facturaService.tieneFacturasPendientes(1L));
    }

    @Test
    @DisplayName("obtenerUltimaFacturaPendienteId retorna ID correcto")
    void testObtenerUltimaFacturaPendienteId() {
        Factura factura = Factura.builder().id(5L).estado(EstadoFactura.PENDIENTE).build();
        when(facturaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaEmisionDesc(1L, EstadoFactura.PENDIENTE))
                .thenReturn(Optional.of(factura));

        Long id = facturaService.obtenerUltimaFacturaPendienteId(1L);
        assertEquals(5L, id);
    }

    @Test
    @DisplayName("obtenerUltimaFacturaPendienteId retorna null si no hay pendientes")
    void testObtenerUltimaFacturaPendienteIdNull() {
        when(facturaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaEmisionDesc(1L, EstadoFactura.PENDIENTE))
                .thenReturn(Optional.empty());

        assertNull(facturaService.obtenerUltimaFacturaPendienteId(1L));
    }

    @Test
    @DisplayName("anularFacturasPendientes cambia estado de todas a ANULADA")
    void testAnularFacturasPendientes() {
        Factura f1 = Factura.builder().id(1L).estado(EstadoFactura.PENDIENTE).build();
        Factura f2 = Factura.builder().id(2L).estado(EstadoFactura.PENDIENTE).build();
        List<Factura> pendientes = Arrays.asList(f1, f2);

        when(facturaRepository.findByUsuarioIdAndEstado(1L, EstadoFactura.PENDIENTE))
                .thenReturn(pendientes);
        when(facturaRepository.saveAll(anyList())).thenReturn(pendientes);

        facturaService.anularFacturasPendientes(1L);

        assertEquals(EstadoFactura.ANULADA, f1.getEstado());
        assertEquals(EstadoFactura.ANULADA, f2.getEstado());
        verify(facturaRepository, times(1)).saveAll(pendientes);
    }

    @Test
    @DisplayName("anularFacturasPendientes sin facturas no genera error")
    void testAnularFacturasPendientesSinFacturas() {
        when(facturaRepository.findByUsuarioIdAndEstado(1L, EstadoFactura.PENDIENTE))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> facturaService.anularFacturasPendientes(1L));
        verify(facturaRepository, times(1)).saveAll(Collections.emptyList());
    }
}
