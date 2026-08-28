package pe.edu.upeu.PharmaBackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.model.Categoria;
import pe.edu.upeu.PharmaBackend.model.Producto;
import pe.edu.upeu.PharmaBackend.repository.CatergoriaRepository;
import pe.edu.upeu.PharmaBackend.repository.ProductoRepository;
import pe.edu.upeu.PharmaBackend.service.service.ProductoService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductoServiceImpl implements ProductoService {
    private final ProductoRepository productoRepository;
    private final CatergoriaRepository catergoriaRepository;

    @Override
    @Transactional
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
        if (productoRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Intento de actualizar a un nombre de producto ya existente: {}", nombre);
            throw new ReglaNegocioException("Ya existe un producto con el nombre " + nombre);
        }

        Categoria categoria = catergoriaRepository.findById(t.getCategoriaId()).orElseThrow(() -> {
            log.error("Error al actualizar: No se encontró la categoría con ID {}", t.getCategoriaId());
            return new RecursoNoEncontradoException("La categoria no se encontró");
        });

        producto.setNombre(nombre);
        producto.setPrecio(t.getPrecio());
        producto.setStock(t.getStock());
        producto.setEstado(t.getEstado());
        producto.setCategoria(categoria);

        Producto prodActualizado = productoRepository.save(producto);
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
    public void eliminar(Long aLong) {
        log.info("Iniciando eliminación de producto con ID: {}", aLong);
        Producto producto = productoRepository.findById(aLong).orElseThrow(() -> {
            log.error("Error al eliminar: Producto no encontrado con ID {}", aLong);
            return new RecursoNoEncontradoException("Producto no encontrado con id: " + aLong);
        });
        productoRepository.delete(producto);
        log.info("Producto con ID {} eliminado exitosamente", aLong);
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
                producto.getCategoria().getNombre()
        );
    }
}
