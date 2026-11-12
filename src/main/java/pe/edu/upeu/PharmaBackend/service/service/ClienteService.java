package pe.edu.upeu.PharmaBackend.service.service;

import java.util.List;
import pe.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.service.generic.CrudService;

public interface ClienteService extends CrudService<ClienteRequestDTO, ClienteResponseDTO, Long> {
    PaginaResponseDTO<ClienteResponseDTO> listarPaginado(int pagina, int tamanio, String ordenarPor, String direccion);
    List<ClienteResponseDTO> buscarActivos(String q);
    void darDeBaja(Long id);
}
