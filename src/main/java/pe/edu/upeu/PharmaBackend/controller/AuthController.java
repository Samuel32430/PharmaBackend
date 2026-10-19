package pe.edu.upeu.PharmaBackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.PharmaBackend.dto.auth.CambioPasswordRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.LoginRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.LoginResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.PharmaBackend.service.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /*
     * Publico. Credenciales correctas -> 200 con el token.
     * Credenciales incorrectas o usuario deshabilitado -> 401.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /*
     * Protegido. Devuelve el usuario dueño del token.
     * Spring inyecta la Authentication que dejo el JwtAuthenticationFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(authService.usuarioActual(authentication.getName()));
    }

    /*
     * Protegido (cualquier rol autenticado).
     * B1: Cambio de contraseña del usuario autenticado.
     * HTTP 204 No Content.
     */
    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambioPasswordRequestDTO request,
            Authentication authentication) {
        authService.cambiarPassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
