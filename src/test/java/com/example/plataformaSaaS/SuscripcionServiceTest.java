package com.example.plataformaSaaS;

import com.example.plataformaSaaS.model.Factura;
import com.example.plataformaSaaS.model.Plan;
import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.Usuario;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import com.example.plataformaSaaS.repository.PlanRepository;
import com.example.plataformaSaaS.repository.SuscripcionRepository;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import com.example.plataformaSaaS.service.FacturaService;
import com.example.plataformaSaaS.service.SuscripcionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuscripcionService - Tests de suscripciones y prorrateo")
class SuscripcionServiceTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private FacturaService facturaService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private SuscripcionService suscripcionService;

    private Usuario usuario;
    private Plan planBasic;
    private Plan planPremium;
    private Plan planEnterprise;
    private Suscripcion suscripcionActiva;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L).nombre("Juan").email("juan@test.com")
                .password("123").pais("España").build();

        planBasic = Plan.builder()
                .id(1L).nombre("Basic").nivel(NivelPlan.BASIC)
                .precioMensual(new BigDecimal("9.99")).activo(true).build();

        planPremium = Plan.builder()
                .id(2L).nombre("Premium").nivel(NivelPlan.PREMIUM)
                .precioMensual(new BigDecimal("29.99")).activo(true).build();

        planEnterprise = Plan.builder()
                .id(3L).nombre("Enterprise").nivel(NivelPlan.ENTERPRISE)
                .precioMensual(new BigDecimal("99.99")).activo(true).build();

        suscripcionActiva = Suscripcion.builder()
                .id(1L).usuario(usuario).plan(planBasic)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now())
                .fechaProximaFacturacion(LocalDate.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("Crear suscripción exitosa genera primera factura")
    void testCrearSuscripcionExitosa() {
        when(suscripcionRepository.findByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> {
                    Suscripcion s = invocation.getArgument(0);
                    s.setId(1L);
                    return s;
                });
        when(facturaService.generarFacturaRenovacion(any(Suscripcion.class)))
                .thenReturn(new Factura());

        Suscripcion nueva = suscripcionService.crearSuscripcion(usuario, planBasic);

        assertNotNull(nueva);
        assertEquals(EstadoSuscripcion.ACTIVA, nueva.getEstado());
        assertEquals(planBasic, nueva.getPlan());
        assertEquals(usuario, nueva.getUsuario());
        assertEquals(LocalDate.now(), nueva.getFechaInicio());
        assertEquals(LocalDate.now().plusDays(30), nueva.getFechaProximaFacturacion());

        verify(facturaService, times(1)).generarFacturaRenovacion(any(Suscripcion.class));
    }

    @Test
    @DisplayName("No se puede crear suscripción si ya existe una activa")
    void testCrearSuscripcionDuplicadaFalla() {
        when(suscripcionRepository.findByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA))
                .thenReturn(Optional.of(suscripcionActiva));

        assertThrows(IllegalStateException.class, () ->
                suscripcionService.crearSuscripcion(usuario, planBasic));

        verify(suscripcionRepository, never()).save(any());
        verify(facturaService, never()).generarFacturaRenovacion(any());
    }

    @Test
    @DisplayName("Upgrade de Basic a Premium genera factura de prorrateo (sin pendientes)")
    void testUpgradeGeneraProrrateo() {
        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(facturaService.tieneFacturasPendientes(1L)).thenReturn(false);
        when(facturaService.generarFacturaProrrateo(any(), any(), any(), any()))
                .thenReturn(new Factura());

        Suscripcion resultado = suscripcionService.cambiarPlan(1L, planPremium);

        assertEquals(planPremium, resultado.getPlan());
        verify(facturaService, times(1))
                .generarFacturaProrrateo(any(), any(), eq("Basic"), eq("Premium"));
        verify(facturaService, never()).anularFacturasPendientes(anyLong());
        verify(facturaService, never()).generarFacturaRenovacion(any());
    }

    @Test
    @DisplayName("Downgrade de Premium a Basic sin pendientes NO genera prorrateo")
    void testDowngradeNoProrrateo() {
        suscripcionActiva.setPlan(planPremium);

        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(facturaService.tieneFacturasPendientes(1L)).thenReturn(false);

        Suscripcion resultado = suscripcionService.cambiarPlan(1L, planBasic);

        assertEquals(planBasic, resultado.getPlan());
        verify(facturaService, never()).generarFacturaProrrateo(any(), any(), any(), any());
        verify(facturaService, never()).anularFacturasPendientes(anyLong());
    }

    @Test
    @DisplayName("Upgrade con facturas pendientes: anula pendientes y genera factura nueva")
    void testUpgradeConFacturasPendientesAnulaYGeneraNueva() {
        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(facturaService.tieneFacturasPendientes(1L)).thenReturn(true);
        when(facturaService.generarFacturaRenovacion(any())).thenReturn(new Factura());

        Suscripcion resultado = suscripcionService.cambiarPlan(1L, planPremium);

        assertEquals(planPremium, resultado.getPlan());
        verify(facturaService, times(1)).anularFacturasPendientes(1L);
        verify(facturaService, times(1)).generarFacturaRenovacion(any());
        verify(facturaService, never()).generarFacturaProrrateo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Downgrade con facturas pendientes: anula pendientes y genera factura nueva")
    void testDowngradeConFacturasPendientesAnulaYGeneraNueva() {
        suscripcionActiva.setPlan(planPremium);

        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(facturaService.tieneFacturasPendientes(1L)).thenReturn(true);
        when(facturaService.generarFacturaRenovacion(any())).thenReturn(new Factura());

        Suscripcion resultado = suscripcionService.cambiarPlan(1L, planBasic);

        assertEquals(planBasic, resultado.getPlan());
        verify(facturaService, times(1)).anularFacturasPendientes(1L);
        verify(facturaService, times(1)).generarFacturaRenovacion(any());
        verify(facturaService, never()).generarFacturaProrrateo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cálculo de prorrateo: Basic->Premium con 30 días restantes")
    void testCalculoProrrateo30Dias() {
        suscripcionActiva.setFechaProximaFacturacion(LocalDate.now().plusDays(30));

        BigDecimal prorrateo = suscripcionService.calcularProrrateo(suscripcionActiva, planPremium);

        BigDecimal esperado = planPremium.getPrecioMensual().subtract(planBasic.getPrecioMensual())
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(esperado, prorrateo);
    }

    @Test
    @DisplayName("Cálculo de prorrateo: Basic->Premium con 15 días restantes")
    void testCalculoProrrateo15Dias() {
        suscripcionActiva.setFechaProximaFacturacion(LocalDate.now().plusDays(15));

        BigDecimal prorrateo = suscripcionService.calcularProrrateo(suscripcionActiva, planPremium);

        BigDecimal precioDiarioBasic = planBasic.getPrecioMensual()
                .divide(new BigDecimal(30), 4, RoundingMode.HALF_UP);
        BigDecimal precioDiarioPremium = planPremium.getPrecioMensual()
                .divide(new BigDecimal(30), 4, RoundingMode.HALF_UP);
        BigDecimal esperado = precioDiarioPremium.subtract(precioDiarioBasic)
                .multiply(new BigDecimal(15)).setScale(2, RoundingMode.HALF_UP);

        assertEquals(esperado, prorrateo);
    }

    @Test
    @DisplayName("Cálculo de prorrateo: Basic->Enterprise con 20 días restantes")
    void testCalculoProrrateoBasicEnterprise() {
        suscripcionActiva.setFechaProximaFacturacion(LocalDate.now().plusDays(20));

        BigDecimal prorrateo = suscripcionService.calcularProrrateo(suscripcionActiva, planEnterprise);

        BigDecimal precioDiarioBasic = planBasic.getPrecioMensual()
                .divide(new BigDecimal(30), 4, RoundingMode.HALF_UP);
        BigDecimal precioDiarioEnterprise = planEnterprise.getPrecioMensual()
                .divide(new BigDecimal(30), 4, RoundingMode.HALF_UP);
        BigDecimal esperado = precioDiarioEnterprise.subtract(precioDiarioBasic)
                .multiply(new BigDecimal(20)).setScale(2, RoundingMode.HALF_UP);

        assertEquals(esperado, prorrateo);
        assertTrue(prorrateo.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Prorrateo es cero cuando no quedan días")
    void testProrrateo0Dias() {
        suscripcionActiva.setFechaProximaFacturacion(LocalDate.now());

        BigDecimal prorrateo = suscripcionService.calcularProrrateo(suscripcionActiva, planPremium);

        assertEquals(BigDecimal.ZERO, prorrateo);
    }

    @Test
    @DisplayName("No se puede cambiar al mismo plan")
    void testCambiarAlMismoPlanFalla() {
        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));

        assertThrows(IllegalArgumentException.class, () ->
                suscripcionService.cambiarPlan(1L, planBasic));
    }

    @Test
    @DisplayName("No se puede cambiar plan de suscripción cancelada")
    void testCambiarPlanSuscripcionCanceladaFalla() {
        suscripcionActiva.setEstado(EstadoSuscripcion.CANCELADA);
        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));

        assertThrows(IllegalStateException.class, () ->
                suscripcionService.cambiarPlan(1L, planPremium));
    }

    @Test
    @DisplayName("No se puede cambiar plan de suscripción inexistente")
    void testCambiarPlanSuscripcionInexistenteFalla() {
        when(suscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                suscripcionService.cambiarPlan(99L, planPremium));
    }

    @Test
    @DisplayName("Cancelar suscripción correctamente")
    void testCancelarSuscripcion() {
        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion cancelada = suscripcionService.cancelarSuscripcion(1L);

        assertEquals(EstadoSuscripcion.CANCELADA, cancelada.getEstado());
        assertNotNull(cancelada.getFechaCancelacion());
        assertNotNull(cancelada.getFechaFin());
    }

    @Test
    @DisplayName("Marcar suscripción como morosa")
    void testMarcarMorosa() {
        when(suscripcionRepository.findById(1L)).thenReturn(Optional.of(suscripcionActiva));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion morosa = suscripcionService.marcarMorosa(1L);

        assertEquals(EstadoSuscripcion.MOROSA, morosa.getEstado());
    }

    @Test
    @DisplayName("Obtener suscripción activa de un usuario")
    void testObtenerSuscripcionActiva() {
        when(suscripcionRepository.findByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA))
                .thenReturn(Optional.of(suscripcionActiva));

        Optional<Suscripcion> resultado = suscripcionService.obtenerSuscripcionActiva(1L);

        assertTrue(resultado.isPresent());
        assertEquals(suscripcionActiva, resultado.get());
    }

    @Test
    @DisplayName("No hay suscripción activa para un usuario sin suscripción")
    void testObtenerSuscripcionActivaNoExiste() {
        when(suscripcionRepository.findByUsuarioIdAndEstado(99L, EstadoSuscripcion.ACTIVA))
                .thenReturn(Optional.empty());

        Optional<Suscripcion> resultado = suscripcionService.obtenerSuscripcionActiva(99L);

        assertTrue(resultado.isEmpty());
    }
}
