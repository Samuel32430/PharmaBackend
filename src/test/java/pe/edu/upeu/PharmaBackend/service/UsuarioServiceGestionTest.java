package pe.edu.upeu.PharmaBackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.PharmaBackend.dto.auth.AsignarRolesRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Empleado;
import pe.edu.upeu.PharmaBackend.entity.Rol;
import pe.edu.upeu.PharmaBackend.entity.Usuario;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.mapper.UsuarioMapper;
import pe.edu.upeu.PharmaBackend.repository.RolRepository;
import pe.edu.upeu.PharmaBackend.repository.UsuarioRepository;
import pe.edu.upeu.PharmaBackend.service.impl.UsuarioServiceImpl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceGestionTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario adminUser;
    private Usuario vendedorUser;
    private Rol rolAdmin;
    private Rol rolVendedor;
    private Rol rolAlmacenero;

    @BeforeEach
    void setUp() {
        Empleado emp1 = new Empleado();
        emp1.setId(1L);
        emp1.setNombres("Admin");
        emp1.setApellidos("General");

        Empleado emp2 = new Empleado();
        emp2.setId(2L);
        emp2.setNombres("Juan");
        emp2.setApellidos("Vendedor");

        rolAdmin = new Rol(1L, "ADMIN", "Administrador");
        rolVendedor = new Rol(2L, "VENDEDOR", "Ventas");
        rolAlmacenero = new Rol(3L, "ALMACENERO", "Almacen");

        adminUser = new Usuario(1L, "admin", "$2a$10$hash", true, emp1, new HashSet<>(Set.of(rolAdmin)), LocalDateTime.now());
        vendedorUser = new Usuario(2L, "vendedor", "$2a$10$hash", true, emp2, new HashSet<>(Set.of(rolVendedor)), LocalDateTime.now());
    }

    @Test
    @DisplayName("Asignación de roles exitosa")
    void asignarRolesExitoso() {
        AsignarRolesRequestDTO request = new AsignarRolesRequestDTO(Set.of(2L, 3L));

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedorUser));
        when(rolRepository.findAllById(request.getRolIds())).thenReturn(List.of(rolVendedor, rolAlmacenero));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(vendedorUser);
        when(usuarioMapper.toUsuarioResponse(any(Usuario.class))).thenReturn(new UsuarioResponseDTO());

        UsuarioResponseDTO response = usuarioService.asignarRoles(2L, request, "admin");

        assertThat(response).isNotNull();
        assertThat(vendedorUser.getRoles()).containsExactlyInAnyOrder(rolVendedor, rolAlmacenero);
        verify(usuarioRepository).save(vendedorUser);
    }

    @Test
    @DisplayName("Asignar roles rechaza si algún rol no existe (HTTP 404)")
    void asignarRolesInexistente() {
        AsignarRolesRequestDTO request = new AsignarRolesRequestDTO(Set.of(2L, 99L));

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedorUser));
        when(rolRepository.findAllById(request.getRolIds())).thenReturn(List.of(rolVendedor));

        assertThatThrownBy(() -> usuarioService.asignarRoles(2L, request, "admin"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Roles no encontrados: [99]");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin no puede despojarse de su propio rol ADMIN (HTTP 409)")
    void adminNoPuedeQuitarseRolAdmin() {
        AsignarRolesRequestDTO request = new AsignarRolesRequestDTO(Set.of(2L)); // Solo rol vendedor

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(rolRepository.findAllById(request.getRolIds())).thenReturn(List.of(rolVendedor));

        assertThatThrownBy(() -> usuarioService.asignarRoles(1L, request, "admin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Un administrador no puede retirarse a sí mismo el rol ADMIN");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivación de usuario exitosa")
    void desactivarUsuarioExitoso() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedorUser));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(vendedorUser);
        when(usuarioMapper.toUsuarioResponse(any(Usuario.class))).thenReturn(new UsuarioResponseDTO());

        usuarioService.cambiarEstado(2L, false, "admin");

        assertThat(vendedorUser.getEstado()).isFalse();
        verify(usuarioRepository).save(vendedorUser);
    }

    @Test
    @DisplayName("Admin no puede desactivar su propia cuenta (HTTP 409)")
    void adminNoPuedeDesactivarseASiMismo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> usuarioService.cambiarEstado(1L, false, "admin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Un administrador no puede desactivar su propia cuenta");

        verify(usuarioRepository, never()).save(any());
    }
}
