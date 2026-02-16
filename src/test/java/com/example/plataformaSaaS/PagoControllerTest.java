package com.example.plataformaSaaS;

import com.example.plataformaSaaS.controller.PagoController;
import com.example.plataformaSaaS.dto.FacturaPagoDTO;
import com.example.plataformaSaaS.dto.MetodoPagoResumenDTO;
import com.example.plataformaSaaS.model.*;
import com.example.plataformaSaaS.model.enums.EstadoFactura;
import com.example.plataformaSaaS.model.enums.TipoPago;
import com.example.plataformaSaaS.service.FacturaService;
import com.example.plataformaSaaS.service.MetodoPagoService;
import com.example.plataformaSaaS.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagoController.class)
@DisplayName("PagoController - Tests de endpoints de pago")
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacturaService facturaService;

    @MockitoBean
    private MetodoPagoService metodoPagoService;

    @MockitoBean
    private UsuarioService usuarioService;

    private MockHttpSession session;
    private Factura facturaPendiente;
    private FacturaPagoDTO facturaPagoDTO;
    private MetodoPagoResumenDTO metodoTarjetaDTO;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .id(1L).nombre("Marco").email("marco@test.com")
                .password("123").pais("España").build();

        facturaPendiente = Factura.builder()
                .id(10L)
                .usuario(usuario)
                .concepto("Renovación mensual - Plan Basic")
                .monto(new BigDecimal("9.99"))
                .impuesto(new BigDecimal("2.10"))
                .montoTotal(new BigDecimal("12.09"))
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .estado(EstadoFactura.PENDIENTE)
                .paisFacturacion("España")
                .build();

        facturaPagoDTO = FacturaPagoDTO.builder()
                .id(10L)
                .concepto("Renovación mensual - Plan Basic")
                .monto(new BigDecimal("9.99"))
                .impuesto(new BigDecimal("2.10"))
                .montoTotal(new BigDecimal("12.09"))
                .tasaImpuesto(new BigDecimal("21.00"))
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .build();

        metodoTarjetaDTO = MetodoPagoResumenDTO.builder()
                .id(1L)
                .tipoPago(TipoPago.TARJETA)
                .numeroTarjeta("4242")
                .titular("Marco Cardo")
                .build();

        session = new MockHttpSession();
        session.setAttribute("usuarioId", 1L);
    }

    @Test
    @DisplayName("GET /factura/{id}/pagar - muestra vista de pago")
    void testMostrarPago() throws Exception {
        when(facturaService.obtenerFacturaParaPago(10L, 1L)).thenReturn(facturaPagoDTO);
        when(metodoPagoService.listarResumenPorUsuario(1L)).thenReturn(List.of(metodoTarjetaDTO));

        mockMvc.perform(get("/factura/10/pagar").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("pagar"))
                .andExpect(model().attributeExists("factura"))
                .andExpect(model().attributeExists("metodosPago"));
    }

    @Test
    @DisplayName("GET /factura/{id}/pagar - redirige si no está logueado")
    void testMostrarPagoSinLogin() throws Exception {
        mockMvc.perform(get("/factura/10/pagar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /factura/{id}/pagar - redirige si factura no existe")
    void testMostrarPagoFacturaNoExiste() throws Exception {
        when(facturaService.obtenerFacturaParaPago(99L, 1L))
                .thenThrow(new IllegalArgumentException("Factura no encontrada"));

        mockMvc.perform(get("/factura/99/pagar").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"));
    }

    @Test
    @DisplayName("GET /factura/{id}/pagar - redirige si factura es de otro usuario")
    void testMostrarPagoFacturaOtroUsuario() throws Exception {
        when(facturaService.obtenerFacturaParaPago(10L, 1L))
                .thenThrow(new IllegalArgumentException("Factura no encontrada"));

        mockMvc.perform(get("/factura/10/pagar").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"));
    }

    @Test
    @DisplayName("GET /factura/{id}/pagar - redirige si factura no está pendiente")
    void testMostrarPagoFacturaNoPendiente() throws Exception {
        when(facturaService.obtenerFacturaParaPago(10L, 1L))
                .thenThrow(new IllegalStateException("Esta factura no se puede pagar"));

        mockMvc.perform(get("/factura/10/pagar").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"));
    }

    @Test
    @DisplayName("POST /factura/{id}/pagar - procesa pago exitosamente")
    void testProcesarPagoExitoso() throws Exception {
        when(facturaService.procesarPago(10L, 1L, 1L)).thenReturn(facturaPendiente);

        mockMvc.perform(post("/factura/10/pagar")
                        .session(session)
                        .param("metodoPagoId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attributeExists("success"));

        verify(facturaService, times(1)).procesarPago(10L, 1L, 1L);
    }

    @Test
    @DisplayName("POST /factura/{id}/pagar - falla con método de pago de otro usuario")
    void testProcesarPagoMetodoOtroUsuario() throws Exception {
        doThrow(new IllegalArgumentException("Método de pago no pertenece al usuario"))
                .when(facturaService).procesarPago(10L, 1L, 1L);

        mockMvc.perform(post("/factura/10/pagar")
                        .session(session)
                        .param("metodoPagoId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /metodos-pago - lista los métodos")
    void testVerMetodosPago() throws Exception {
        when(metodoPagoService.listarResumenPorUsuario(1L)).thenReturn(List.of(metodoTarjetaDTO));

        mockMvc.perform(get("/metodos-pago").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("metodos-pago"))
                .andExpect(model().attributeExists("metodosPago"))
                .andExpect(model().attributeExists("tarjetaRequest"))
                .andExpect(model().attributeExists("payPalRequest"))
                .andExpect(model().attributeExists("transferenciaRequest"));
    }

    @Test
    @DisplayName("POST /metodos-pago/tarjeta - agrega tarjeta")
    void testAgregarTarjeta() throws Exception {
        PagoTarjeta tarjetaGuardada = new PagoTarjeta();
        tarjetaGuardada.setId(1L);
        when(metodoPagoService.guardarTarjetaPorUsuarioId(eq(1L), any(), any(), any()))
                .thenReturn(tarjetaGuardada);

        mockMvc.perform(post("/metodos-pago/tarjeta")
                        .session(session)
                        .param("numeroTarjeta", "4111111111111234")
                        .param("titular", "MARCO CARDO CABALERO")
                        .param("fechaExpiracion", "12/28"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/metodos-pago"))
                .andExpect(flash().attributeExists("success"));

        verify(metodoPagoService, times(1)).guardarTarjetaPorUsuarioId(eq(1L),
                eq("4111111111111234"), eq("MARCO CARDO CABALERO"), eq("12/28"));
    }

    @Test
    @DisplayName("POST /metodos-pago/paypal - agrega PayPal")
    void testAgregarPayPal() throws Exception {
        PagoPayPal paypal = new PagoPayPal();
        paypal.setId(2L);
        when(metodoPagoService.guardarPayPalPorUsuarioId(eq(1L), any())).thenReturn(paypal);

        mockMvc.perform(post("/metodos-pago/paypal")
                        .session(session)
                        .param("emailPaypal", "marco@paypal.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/metodos-pago"))
                .andExpect(flash().attributeExists("success"));

        verify(metodoPagoService, times(1)).guardarPayPalPorUsuarioId(eq(1L), eq("marco@paypal.com"));
    }

    @Test
    @DisplayName("POST /metodos-pago/transferencia - agrega transferencia")
    void testAgregarTransferencia() throws Exception {
        PagoTransferencia transferencia = new PagoTransferencia();
        transferencia.setId(3L);
        when(metodoPagoService.guardarTransferenciaPorUsuarioId(eq(1L), any(), any()))
                .thenReturn(transferencia);

        mockMvc.perform(post("/metodos-pago/transferencia")
                        .session(session)
                        .param("numeroCuenta", "ES7620770024003102575766")
                        .param("banco", "Banco Santander"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/metodos-pago"))
                .andExpect(flash().attributeExists("success"));

        verify(metodoPagoService, times(1)).guardarTransferenciaPorUsuarioId(eq(1L),
                eq("ES7620770024003102575766"), eq("Banco Santander"));
    }

    @Test
    @DisplayName("POST /metodos-pago/eliminar/{id} - elimina método propio")
    void testEliminarMetodoPagoPropio() throws Exception {
        doNothing().when(metodoPagoService).eliminarPorUsuario(1L, 1L);

        mockMvc.perform(post("/metodos-pago/eliminar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/metodos-pago"))
                .andExpect(flash().attributeExists("success"));

        verify(metodoPagoService, times(1)).eliminarPorUsuario(1L, 1L);
    }

    @Test
    @DisplayName("POST /metodos-pago/eliminar/{id} - no puede eliminar método de otro usuario")
    void testEliminarMetodoPagoAjeno() throws Exception {
        doThrow(new IllegalArgumentException("No autorizado"))
                .when(metodoPagoService).eliminarPorUsuario(1L, 1L);

        mockMvc.perform(post("/metodos-pago/eliminar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/metodos-pago"))
                .andExpect(flash().attributeExists("error"));
    }
}
