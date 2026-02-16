package com.example.plataformaSaaS;

import com.example.plataformaSaaS.model.Plan;
import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.Usuario;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import com.example.plataformaSaaS.repository.SuscripcionRepository;
import com.example.plataformaSaaS.service.FacturaService;
import com.example.plataformaSaaS.service.ScheduledBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledBillingService - Tests de facturación automática")
class ScheduledBillingServiceTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private FacturaService facturaService;

    @InjectMocks
    private ScheduledBillingService scheduledBillingService;

    private Usuario usuario1;
    private Usuario usuario2;
    private Plan planBasic;
    private Plan planPremium;
    private Suscripcion suscripcion1;
    private Suscripcion suscripcion2;

    @BeforeEach
    void setUp() {
        usuario1 = Usuario.builder()
                .id(1L).nombre("Juan").email("juan@test.com")
                .password("123").pais("España").build();

        usuario2 = Usuario.builder()
                .id(2L).nombre("María").email("maria@test.com")
                .password("123").pais("México").build();

        planBasic = Plan.builder()
                .id(1L).nombre("Basic").nivel(NivelPlan.BASIC)
                .precioMensual(new BigDecimal("9.99")).activo(true).build();

        planPremium = Plan.builder()
                .id(2L).nombre("Premium").nivel(NivelPlan.PREMIUM)
                .precioMensual(new BigDecimal("29.99")).activo(true).build();

        suscripcion1 = Suscripcion.builder()
                .id(1L).usuario(usuario1).plan(planBasic)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now().minusDays(30))
                .fechaProximaFacturacion(LocalDate.now())
                .build();

        suscripcion2 = Suscripcion.builder()
                .id(2L).usuario(usuario2).plan(planPremium)
                .estado(EstadoSuscripcion.ACTIVA)
                .fechaInicio(LocalDate.now().minusDays(30))
                .fechaProximaFacturacion(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Facturación automática procesa suscripciones vencidas")
    void testFacturacionProcesaSuscripcionesVencidas() {
        List<Suscripcion> vencidas = Arrays.asList(suscripcion1, suscripcion2);

        when(suscripcionRepository.findByEstadoAndFechaProximaFacturacionLessThanEqual(
                EstadoSuscripcion.ACTIVA, LocalDate.now()))
                .thenReturn(vencidas);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int facturasGeneradas = scheduledBillingService.ejecutarFacturacionManual();

        assertEquals(2, facturasGeneradas);
        verify(facturaService, times(2)).generarFacturaRenovacion(any());
        verify(suscripcionRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Facturación actualiza fecha de próxima facturación a +30 días")
    void testFacturacionActualizaFechaProxima() {
        when(suscripcionRepository.findByEstadoAndFechaProximaFacturacionLessThanEqual(
                EstadoSuscripcion.ACTIVA, LocalDate.now()))
                .thenReturn(Collections.singletonList(suscripcion1));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        scheduledBillingService.ejecutarFacturacionManual();

        assertEquals(LocalDate.now().plusDays(30), suscripcion1.getFechaProximaFacturacion());
    }

    @Test
    @DisplayName("No genera facturas cuando no hay suscripciones vencidas")
    void testSinSuscripcionesVencidas() {
        when(suscripcionRepository.findByEstadoAndFechaProximaFacturacionLessThanEqual(
                EstadoSuscripcion.ACTIVA, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        int facturasGeneradas = scheduledBillingService.ejecutarFacturacionManual();

        assertEquals(0, facturasGeneradas);
        verify(facturaService, never()).generarFacturaRenovacion(any());
    }

    @Test
    @DisplayName("Error en una suscripción no detiene el proceso de las demás")
    void testErrorNoDetieneFacturacion() {
        List<Suscripcion> vencidas = Arrays.asList(suscripcion1, suscripcion2);

        when(suscripcionRepository.findByEstadoAndFechaProximaFacturacionLessThanEqual(
                EstadoSuscripcion.ACTIVA, LocalDate.now()))
                .thenReturn(vencidas);

        when(facturaService.generarFacturaRenovacion(suscripcion1))
                .thenThrow(new RuntimeException("Error de prueba"));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int facturasGeneradas = scheduledBillingService.ejecutarFacturacionManual();

        assertEquals(1, facturasGeneradas);
    }
}
