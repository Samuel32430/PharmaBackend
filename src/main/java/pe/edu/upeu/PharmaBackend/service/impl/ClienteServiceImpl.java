package pe.edu.upeu.PharmaBackend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.model.Cliente;
import pe.edu.upeu.PharmaBackend.repository.ClienteRepository;
import pe.edu.upeu.PharmaBackend.service.service.ClienteService;
import pe.edu.upeu.PharmaBackend.util.PaginacionUtil;

import java.util.List;
import java.util.Set;

@Service
public class ClienteServiceImpl implements ClienteService {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteServiceImpl.class);

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("id", "dni", "nombres", "apellidos", "email");

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public ClienteResponseDTO guardar(ClienteRequestDTO request) {
        log.info("Registrando cliente con DNI={}", request.getDni());

        String dni = request.getDni().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (clienteRepository.existsByDni(dni)) {
            throw new ReglaNegocioException("Ya existe un cliente con el DNI: " + dni);
        }

        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe un cliente con el correo: " + email);
        }

        Cliente cliente = new Cliente();
        cliente.setDni(dni);
        cliente.setNombres(request.getNombres().trim());
        cliente.setApellidos(request.getApellidos().trim());
        cliente.setEmail(email);
        cliente.setTelefono(normalizar(request.getTelefono()));
        cliente.setDireccion(normalizar(request.getDireccion()));
        cliente.setEstado(request.getEstado());

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente registrado correctamente id={}", guardado.getId());
        return convertirResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        log.info("Buscando cliente id={}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + id));
        return convertirResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listar() {
        log.info("Listando clientes");
        return clienteRepository.findAll()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + id));

        String dni = request.getDni().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (clienteRepository.existsByDniAndIdNot(dni, id)) {
            throw new ReglaNegocioException("Ya existe otro cliente con el DNI: " + dni);
        }

        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ReglaNegocioException("Ya existe otro cliente con el correo: " + email);
        }

        cliente.setDni(dni);
        cliente.setNombres(request.getNombres().trim());
        cliente.setApellidos(request.getApellidos().trim());
        cliente.setEmail(email);
        cliente.setTelefono(normalizar(request.getTelefono()));
        cliente.setDireccion(normalizar(request.getDireccion()));
        cliente.setEstado(request.getEstado());

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Cliente id={} actualizado correctamente", id);
        return convertirResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + id));
        clienteRepository.delete(cliente);
        log.info("Cliente id={} eliminado correctamente", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ClienteResponseDTO> listarPaginado(
            int pagina, int tamanio, String ordenarPor, String direccion) {
        log.info("Listando clientes paginado: pagina={}, tamanio={}, ordenarPor={}, direccion={}",
                pagina, tamanio, ordenarPor, direccion);

        Pageable pageable = PaginacionUtil.construir(
                pagina, tamanio, ordenarPor, direccion, CAMPOS_ORDENABLES, "apellidos");
        Page<Cliente> page = clienteRepository.findAll(pageable);

        return PaginaResponseDTO.de(page.map(this::convertirResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarActivos(String q) {
        String filtro = (q == null || q.isBlank()) ? null : q.trim();
        Pageable limite = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "apellidos"));
        return clienteRepository.buscarActivos(filtro, limite)
                .getContent()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    @Transactional
    public void darDeBaja(Long id) {
        log.info("Dando de baja lógica al cliente id={}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + id));

        if (Boolean.FALSE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException("El cliente con id " + id + " ya se encuentra inactivo");
        }

        cliente.setEstado(false);
        clienteRepository.save(cliente);
        log.info("Cliente id={} dado de baja exitosamente", id);
    }

    private ClienteResponseDTO convertirResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getDni(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getEstado(),
                cliente.getFechaCreacion(),
                cliente.getFechaModificacion(),
                cliente.getCreadoPor(),
                cliente.getModificadoPor()
        );
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
