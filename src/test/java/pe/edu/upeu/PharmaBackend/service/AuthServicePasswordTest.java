package pe.edu.upeu.PharmaBackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upeu.PharmaBackend.dto.auth.CambioPasswordRequestDTO;
import pe.edu.upeu.PharmaBackend.entity.Usuario;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.repository.UsuarioRepository;
import pe.edu.upeu.PharmaBackend.service.impl.AuthServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("vendedor");
        usuario.setPassword("$2a$10$hashActual");
        usuario.setEstado(true);
    }

    @Test
    @DisplayName("Cambio de contraseña exitoso cuando los datos son válidos")
    void cambioPasswordExitoso() {
        CambioPasswordRequestDTO request = new CambioPasswordRequestDTO(
                "Vendedor2026*", "NuevaClave2026");

        when(usuarioRepository.findByUsernameIgnoreCase("vendedor"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Vendedor2026*", "$2a$10$hashActual"))
                .thenReturn(true);
        when(passwordEncoder.matches("NuevaClave2026", "$2a$10$hashActual"))
                .thenReturn(false);
        when(passwordEncoder.encode("NuevaClave2026"))
                .thenReturn("$2a$10$nuevoHash");

        authService.cambiarPassword("vendedor", request);

        assertThat(usuario.getPassword()).isEqualTo("$2a$10$nuevoHash");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Rechaza cambio si la contraseña actual no coincide (HTTP 409)")
    void passwordActualIncorrecta() {
        CambioPasswordRequestDTO request = new CambioPasswordRequestDTO(
                "ClaveErronea123", "NuevaClave2026");

        when(usuarioRepository.findByUsernameIgnoreCase("vendedor"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveErronea123", "$2a$10$hashActual"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.cambiarPassword("vendedor", request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La contraseña actual no es correcta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cambio si la nueva contraseña es igual a la actual (HTTP 409)")
    void passwordNuevaIgualALaActual() {
        CambioPasswordRequestDTO request = new CambioPasswordRequestDTO(
                "Vendedor2026*", "Vendedor2026*");

        when(usuarioRepository.findByUsernameIgnoreCase("vendedor"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Vendedor2026*", "$2a$10$hashActual"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.cambiarPassword("vendedor", request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nueva contraseña debe ser distinta de la actual");

        verify(usuarioRepository, never()).save(any());
    }
}
