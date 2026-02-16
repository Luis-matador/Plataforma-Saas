package com.example.plataformaSaaS;

import com.example.plataformaSaaS.model.*;
import com.example.plataformaSaaS.model.enums.TipoPago;
import com.example.plataformaSaaS.repository.MetodoPagoRepository;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import com.example.plataformaSaaS.service.MetodoPagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetodoPagoService - Tests de métodos de pago")
class MetodoPagoServiceTest {

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MetodoPagoService metodoPagoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Marco Cardo")
                .email("marco@test.com")
                .password("123")
                .pais("España")
                .build();
    }

    @Test
    @DisplayName("Guardar tarjeta almacena solo los últimos 4 dígitos")
    void testGuardarTarjetaUltimos4Digitos() {
        when(metodoPagoRepository.save(any(PagoTarjeta.class)))
                .thenAnswer(invocation -> {
                    PagoTarjeta p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        MetodoPago resultado = metodoPagoService.guardarTarjeta(
                usuario, "4111111111111234", "MARCO CARDO CABALERO", "12/28");

        assertNotNull(resultado);
        assertTrue(resultado instanceof PagoTarjeta);
        PagoTarjeta tarjeta = (PagoTarjeta) resultado;
        assertEquals("1234", tarjeta.getNumeroTarjeta());
        assertEquals("MARCO CARDO CABALERO", tarjeta.getTitular());
        assertEquals("12/28", tarjeta.getFechaExpiracion());
        assertEquals(TipoPago.TARJETA, tarjeta.getTipoPago());
        assertTrue(tarjeta.isActivo());
        assertEquals(usuario, tarjeta.getUsuario());
    }

    @Test
    @DisplayName("Guardar tarjeta con menos de 4 dígitos almacena el número completo")
    void testGuardarTarjetaCorta() {
        when(metodoPagoRepository.save(any(PagoTarjeta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MetodoPago resultado = metodoPagoService.guardarTarjeta(
                usuario, "1234", "MARCO CARDO CABALERO", "06/25");

        PagoTarjeta tarjeta = (PagoTarjeta) resultado;
        assertEquals("1234", tarjeta.getNumeroTarjeta());
    }

    @Test
    @DisplayName("Guardar tarjeta con exactamente 4 dígitos")
    void testGuardarTarjetaExacto4Digitos() {
        when(metodoPagoRepository.save(any(PagoTarjeta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MetodoPago resultado = metodoPagoService.guardarTarjeta(
                usuario, "5678", "MARCO CARDO CABALERO", "03/26");

        PagoTarjeta tarjeta = (PagoTarjeta) resultado;
        assertEquals("5678", tarjeta.getNumeroTarjeta());
    }

    @Test
    @DisplayName("Guardar tarjeta invoca save del repositorio")
    void testGuardarTarjetaInvocaSave() {
        when(metodoPagoRepository.save(any(PagoTarjeta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        metodoPagoService.guardarTarjeta(usuario, "4111111111111234", "Test", "12/28");

        ArgumentCaptor<PagoTarjeta> captor = ArgumentCaptor.forClass(PagoTarjeta.class);
        verify(metodoPagoRepository, times(1)).save(captor.capture());

        PagoTarjeta guardada = captor.getValue();
        assertEquals("1234", guardada.getNumeroTarjeta());
        assertEquals(TipoPago.TARJETA, guardada.getTipoPago());
    }

    @Test
    @DisplayName("Guardar PayPal correctamente")
    void testGuardarPayPal() {
        when(metodoPagoRepository.save(any(PagoPayPal.class)))
                .thenAnswer(invocation -> {
                    PagoPayPal p = invocation.getArgument(0);
                    p.setId(2L);
                    return p;
                });

        MetodoPago resultado = metodoPagoService.guardarPayPal(usuario, "marco@paypal.com");

        assertNotNull(resultado);
        assertTrue(resultado instanceof PagoPayPal);
        PagoPayPal paypal = (PagoPayPal) resultado;
        assertEquals("marco@paypal.com", paypal.getEmailPaypal());
        assertEquals(TipoPago.PAYPAL, paypal.getTipoPago());
        assertTrue(paypal.isActivo());
        assertEquals(usuario, paypal.getUsuario());

        verify(metodoPagoRepository, times(1)).save(any(PagoPayPal.class));
    }

    @Test
    @DisplayName("Guardar transferencia bancaria correctamente")
    void testGuardarTransferencia() {
        when(metodoPagoRepository.save(any(PagoTransferencia.class)))
                .thenAnswer(invocation -> {
                    PagoTransferencia p = invocation.getArgument(0);
                    p.setId(3L);
                    return p;
                });

        MetodoPago resultado = metodoPagoService.guardarTransferencia(
                usuario, "ES7620770024003102575766", "Banco Santander");

        assertNotNull(resultado);
        assertTrue(resultado instanceof PagoTransferencia);
        PagoTransferencia transferencia = (PagoTransferencia) resultado;
        assertEquals("ES7620770024003102575766", transferencia.getNumeroCuenta());
        assertEquals("Banco Santander", transferencia.getBanco());
        assertEquals(TipoPago.TRANSFERENCIA, transferencia.getTipoPago());
        assertTrue(transferencia.isActivo());
        assertEquals(usuario, transferencia.getUsuario());

        verify(metodoPagoRepository, times(1)).save(any(PagoTransferencia.class));
    }

    @Test
    @DisplayName("Listar métodos de pago de un usuario con resultados")
    void testListarPorUsuarioConResultados() {
        PagoTarjeta tarjeta = new PagoTarjeta();
        tarjeta.setId(1L);
        tarjeta.setTipoPago(TipoPago.TARJETA);

        PagoPayPal paypal = new PagoPayPal();
        paypal.setId(2L);
        paypal.setTipoPago(TipoPago.PAYPAL);

        when(metodoPagoRepository.findByUsuarioId(1L))
                .thenReturn(Arrays.asList(tarjeta, paypal));

        List<MetodoPago> metodos = metodoPagoService.listarPorUsuario(1L);

        assertEquals(2, metodos.size());
        verify(metodoPagoRepository, times(1)).findByUsuarioId(1L);
    }

    @Test
    @DisplayName("Listar métodos de pago de un usuario sin métodos")
    void testListarPorUsuarioVacio() {
        when(metodoPagoRepository.findByUsuarioId(99L)).thenReturn(Collections.emptyList());

        List<MetodoPago> metodos = metodoPagoService.listarPorUsuario(99L);

        assertTrue(metodos.isEmpty());
    }

    @Test
    @DisplayName("Buscar método de pago por ID existente")
    void testBuscarPorIdExistente() {
        PagoTarjeta tarjeta = new PagoTarjeta();
        tarjeta.setId(1L);
        tarjeta.setTipoPago(TipoPago.TARJETA);

        when(metodoPagoRepository.findById(1L)).thenReturn(Optional.of(tarjeta));

        Optional<MetodoPago> resultado = metodoPagoService.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals(1L, resultado.get().getId());
    }

    @Test
    @DisplayName("Buscar método de pago por ID inexistente")
    void testBuscarPorIdInexistente() {
        when(metodoPagoRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<MetodoPago> resultado = metodoPagoService.buscarPorId(99L);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Eliminar método de pago por ID")
    void testEliminar() {
        doNothing().when(metodoPagoRepository).deleteById(1L);

        assertDoesNotThrow(() -> metodoPagoService.eliminar(1L));

        verify(metodoPagoRepository, times(1)).deleteById(1L);
    }
}
