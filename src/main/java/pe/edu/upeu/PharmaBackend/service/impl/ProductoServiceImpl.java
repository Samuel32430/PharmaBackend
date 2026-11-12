package pe.edu.upeu.PharmaBackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.auditoria.EventoAuditoria;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.model.Categoria;
import pe.edu.upeu.PharmaBackend.model.Producto;
import pe.edu.upeu.PharmaBackend.repository.CatergoriaRepository;
import pe.edu.upeu.PharmaBackend.repository.ProductoRepository;
import pe.edu.upeu.PharmaBackend.service.service.ProductoService;
import pe.edu.upeu.PharmaBackend.util.PaginacionUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "nombre", "precio", "stock");

    private final ProductoRepository productoRepository;
    private final CatergoriaRepository catergoriaRepository;
    private final ApplicationEventPublisher eventos;

    @Override
    @Transactional
    @CacheEvict(value = "productos", allEntries = true)
    public ProductoResponseDTO guardar(ProductoRequestDTO t) {
        log.info("Iniciando registro de nuevo producto: {}", t.getNombre());
        String nombre = t.getNombre().trim();
        if (nombre.isEmpty()) {
            log.warn("Intento de registrar producto con nombre vacío");
            throw new ReglaNegocioException("El nombre del producto no puede estar vacio");
        }
        if (productoRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Intento de registrar producto duplicado: {}", nombre);
            throw new ReglaNegocioException("Ya existe un producto con el nombre " + nombre);
        }

        Categoria categoria = catergoriaRepository.findById(t.getCategoriaId()).orElseThrow(() -> {
            log.error("Error al registrar producto: No se encontró la categoría con ID {}", t.getCategoriaId());
            return new RecursoNoEncontradoException("La categoria no se encontró");
        });

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(t.getPrecio());
        producto.setEstado(t.getEstado());
        producto.setStock(t.getStock());
        producto.setCategoria(categoria);

        Producto prodCreado = productoRepository.save(producto);
        log.info("Producto registrado exitosamente con ID: {}", prodCreado.getId());
        return convertirResponse(prodCreado);
    }

    @Override
    @Transactional
    @CacheEvict(value = "productos", allEntries = true)
    public ProductoResponseDTO actualizar(Long aLong, ProductoRequestDTO t) {
        log.info("Iniciando actualización del producto con ID: {}", aLong);
        Producto producto = productoRepository.findById(aLong).orElseThrow(() -> {
            log.error("Error al actualizar: Producto no encontrado con ID {}", aLong);
            return new RecursoNoEncontradoException("Producto no encontrada con id: " + aLong);
        });

        String nombre = t.getNombre().trim();
        if (nombre.isEmpty()) {
            log.warn("Intento de actualizar producto con nombre vacío en ID: {}", aLong);
            throw new ReglaNegocioException("El nombre del producto no puede estar vacio");
        }
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, aLong)) {
            log.warn("Intento de actualizar con nombre duplicado: {}", nombre);
            throw new ReglaNegocioException("Ya existe un producto con el nombre " + nombre);
        }

        Categoria categoria = catergoriaRepository.findById(t.getCategoriaId()).orElseThrow(() -> {
            log.error("Error al actualizar: No se encontró la categoría con ID {}", t.getCategoriaId());
            return new RecursoNoEncontradoException("La categoria no se encontró");
        });

        BigDecimal precioAnterior = producto.getPrecio();

        producto.setNombre(nombre);
        producto.setPrecio(t.getPrecio());
        producto.setStock(t.getStock());
        producto.setEstado(t.getEstado());
        producto.setCategoria(categoria);

        Producto prodActualizado = productoRepository.save(producto);
        if (precioAnterior.compareTo(prodActualizado.getPrecio()) != 0) {
            eventos.publishEvent(EventoAuditoria.de(
                    "PRECIO_MODIFICADO",
                    "Producto",
                    prodActualizado.getId(),
                    precioAnterior + " -> " + prodActualizado.getPrecio()));
        }

        log.info("Producto con ID {} actualizado exitosamente", aLong);
        return convertirResponse(prodActualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO buscarPorId(Long aLong) {
        log.info("Buscando producto con ID: {}", aLong);
        Producto producto = productoRepository.findById(aLong).orElseThrow(() -> {
            log.warn("Producto no encontrado con ID: {}", aLong);
            return new RecursoNoEncontradoException("Producto no encontrado con id: " + aLong);
        });
        return convertirResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listar() {
        log.info("Listando todos los productos");
        return productoRepository.findAll()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "productos", allEntries = true)
    public void eliminar(Long aLong) {
        log.info("Iniciando eliminación de producto con ID: {}", aLong);
        Producto producto = productoRepository.findById(aLong).orElseThrow(() -> {
            log.error("Error al eliminar: Producto no encontrado con ID {}", aLong);
            return new RecursoNoEncontradoException("Producto no encontrado con id: " + aLong);
        });
        productoRepository.delete(producto);
        eventos.publishEvent(EventoAuditoria.de(
                "PRODUCTO_ELIMINADO",
                "Producto",
                producto.getId(),
                producto.getNombre()));
        log.info("Producto con ID {} eliminado exitosamente", aLong);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "productos",
            key = "#pagina + '-' + #tamanio + '-' + #ordenarPor + '-' + #direccion")
    public PaginaResponseDTO<ProductoResponseDTO> listarPaginado(
            int pagina, int tamanio, String ordenarPor, String direccion) {
        log.info("Listando productos paginado: pagina={}, tamanio={}, ordenarPor={}, direccion={}",
                pagina, tamanio, ordenarPor, direccion);

        Pageable pageable = PaginacionUtil.construir(
                pagina, tamanio, ordenarPor, direccion, CAMPOS_ORDENABLES, "nombre");
        Page<Producto> page = productoRepository.findAll(pageable);

        return PaginaResponseDTO.de(page.map(this::convertirResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> buscarDisponibles(String q) {
        String filtro = (q == null || q.isBlank()) ? null : q.trim();
        Pageable limite = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "nombre"));
        return productoRepository.buscarDisponibles(filtro, limite)
                .getContent()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "productos", allEntries = true)
    public void darDeBaja(Long id) {
        log.info("Dando de baja lógica al producto id={}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con id: " + id));

        if (Boolean.FALSE.equals(producto.getEstado())) {
            throw new ReglaNegocioException("El producto con id " + id + " ya se encuentra inactivo");
        }

        producto.setEstado(false);
        productoRepository.save(producto);
        eventos.publishEvent(EventoAuditoria.de(
                "PRODUCTO_ELIMINADO",
                "Producto",
                producto.getId(),
                producto.getNombre()));
        log.info("Producto id={} dado de baja exitosamente", id);
    }

    private ProductoResponseDTO convertirResponse(Producto producto) {
        return new ProductoResponseDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getEstado(),
                producto.getFechaCreacion(),
                producto.getFechaModificacion(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getCreadoPor(),
                producto.getModificadoPor()
        );
    }
}
