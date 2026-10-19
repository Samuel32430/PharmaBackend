package pe.edu.upeu.PharmaBackend.service.service;

import pe.edu.upeu.PharmaBackend.dto.auth.AsignarRolesRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.EmpleadoResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.RolResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {
    UsuarioResponseDTO crear(UsuarioRequestDTO request);
    List<UsuarioResponseDTO> listar();
    List<EmpleadoResponseDTO> listarEmpleados();
    List<RolResponseDTO> listarRoles();
    UsuarioResponseDTO asignarRoles(Long id, AsignarRolesRequestDTO request, String authUsername);
    UsuarioResponseDTO cambiarEstado(Long id, Boolean estado, String authUsername);
}
