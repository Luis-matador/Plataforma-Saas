package com.example.plataformaSaaS;

import com.example.plataformaSaaS.model.Usuario;
import com.example.plataformaSaaS.model.enums.EstadoSuscripcion;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import com.example.plataformaSaaS.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Tests de registro y login")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private static final String RAW_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPasswordSimulado123456789012345678";

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan García")
                .email("juan@test.com")
                .password(HASHED_PASSWORD)
                .pais("España")
                .build();
    }

    @Test
    @DisplayName("Registro exitoso de usuario - contraseña se hashea con BCrypt")
    void testRegistroExitoso() {
        Usuario usuarioSinHash = Usuario.builder()
                .nombre("Juan García")
                .email("juan@test.com")
                .password(RAW_PASSWORD)
                .pais("España")
                .build();

        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Usuario resultado = usuarioService.registrar(usuarioSinHash);

        assertNotNull(resultado);
        assertEquals("Juan García", resultado.getNombre());
        assertEquals("juan@test.com", resultado.getEmail());
        verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Registro falla con email duplicado")
    void testRegistroEmailDuplicado() {
        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar(usuario));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login exitoso con credenciales correctas - verifica hash BCrypt")
    void testLoginExitoso() {
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(eq(RAW_PASSWORD), eq(HASHED_PASSWORD))).thenReturn(true);

        Optional<Usuario> resultado = usuarioService.login("juan@test.com", RAW_PASSWORD);

        assertTrue(resultado.isPresent());
        assertEquals("Juan García", resultado.get().getNombre());
        verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
    }

    @Test
    @DisplayName("Login falla con contraseña incorrecta")
    void testLoginPasswordIncorrecta() {
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(eq("wrongpassword"), eq(HASHED_PASSWORD))).thenReturn(false);

        Optional<Usuario> resultado = usuarioService.login("juan@test.com", "wrongpassword");

        assertTrue(resultado.isEmpty());
        verify(passwordEncoder).matches("wrongpassword", HASHED_PASSWORD);
    }

    @Test
    @DisplayName("Login falla con email inexistente")
    void testLoginEmailInexistente() {
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        Optional<Usuario> resultado = usuarioService.login("noexiste@test.com", RAW_PASSWORD);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Buscar usuario por ID existente")
    void testBuscarPorIdExistente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals(1L, resultado.get().getId());
    }

    @Test
    @DisplayName("Buscar usuario por ID inexistente")
    void testBuscarPorIdInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Usuario> resultado = usuarioService.buscarPorId(99L);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Buscar usuario por email")
    void testBuscarPorEmail() {
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.buscarPorEmail("juan@test.com");

        assertTrue(resultado.isPresent());
        assertEquals("juan@test.com", resultado.get().getEmail());
    }

    @Test
    @DisplayName("Estados de suscripción correctos")
    void testEstadosSuscripcion() {
        assertEquals("Activa", EstadoSuscripcion.ACTIVA.getDescripcion());
        assertEquals("Cancelada", EstadoSuscripcion.CANCELADA.getDescripcion());
        assertEquals("Morosa", EstadoSuscripcion.MOROSA.getDescripcion());
    }

    @Test
    @DisplayName("Niveles de plan con orden correcto")
    void testNivelesPlan() {
        assertTrue(NivelPlan.PREMIUM.esSuperiorA(NivelPlan.BASIC));
        assertTrue(NivelPlan.ENTERPRISE.esSuperiorA(NivelPlan.PREMIUM));
        assertTrue(NivelPlan.ENTERPRISE.esSuperiorA(NivelPlan.BASIC));
        assertFalse(NivelPlan.BASIC.esSuperiorA(NivelPlan.PREMIUM));
        assertFalse(NivelPlan.BASIC.esSuperiorA(NivelPlan.BASIC));
    }
}
