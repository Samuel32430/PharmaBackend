package pe.edu.upeu.PharmaBackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.model.Categoria;
import pe.edu.upeu.PharmaBackend.repository.CatergoriaRepository;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CatergoriaRepository catergoriaRepository;

    @Override
    @Transactional
    public CategoriaResponseDTO guardar(CategoriaRequestDTO t) {
        log.info("Iniciando registro de nueva categoría: {}", t.getNombre());
        String nombre = t.getNombre().trim();
        if (nombre.isEmpty()) {
            log.warn("Intento de registrar categoría con nombre vacío");
            throw new ReglaNegocioException("El nombre de la categoria no puede estar vacio");
        }
        if (catergoriaRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Intento de registrar categoría duplicada: {}", nombre);
            throw new ReglaNegocioException("Ya existe una categoria con el nombre " + nombre);
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(t.getDescripcion());
        categoria.setEstado(t.getEstado());

        Categoria catCreada = catergoriaRepository.save(categoria);
        log.info("Categoría registrada exitosamente con ID: {}", catCreada.getId());
        return convertirResponse(catCreada);
    }

    @Override
    @Transactional
    public CategoriaResponseDTO actualizar(Long aLong, CategoriaRequestDTO t) {
        log.info("Iniciando actualización de la categoría con ID: {}", aLong);
        Categoria categoria = catergoriaRepository.findById(aLong).orElseThrow(() -> {
            log.error("Error al actualizar: Categoría no encontrada con ID {}", aLong);
            return new RecursoNoEncontradoException("Categoria no encontrada con id: " + aLong);
        });

        String nombre = t.getNombre().trim();
        if (nombre.isEmpty()) {
            log.warn("Intento de actualizar categoría con nombre vacío en ID: {}", aLong);
            throw new ReglaNegocioException("El nombre de la categoria no puede estar vacio");
        }
        if (catergoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, aLong)) {
            log.warn("Intento de actualizar a un nombre de categoría ya existente: {}", nombre);
            throw new ReglaNegocioException("Ya existe otra categoria con el nombre " + nombre);
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(t.getDescripcion());
        categoria.setEstado(t.getEstado());

        Categoria catActualizada = catergoriaRepository.save(categoria);
        log.info("Categoría con ID {} actualizada exitosamente", aLong);
        return convertirResponse(catActualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long aLong) {
        log.info("Buscando categoría con ID: {}", aLong);
        Categoria categoria = catergoriaRepository.findById(aLong).orElseThrow(() -> {
            log.warn("Categoría no encontrada con ID: {}", aLong);
            return new RecursoNoEncontradoException("Categoria no encontrada con id: " + aLong);
        });
        return convertirResponse(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar() {
        log.info("Listando todas las categorías");
        return catergoriaRepository.findAll()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void eliminar(Long aLong) {
        log.info("Iniciando eliminación de la categoría con ID: {}", aLong);
        Categoria categoria = catergoriaRepository.findById(aLong).orElseThrow(() -> {
            log.error("Error al eliminar: Categoría no encontrada con ID {}", aLong);
            return new RecursoNoEncontradoException("Categoria no encontrada con id: " + aLong);
        });
        catergoriaRepository.delete(categoria);
        log.info("Categoría con ID {} eliminada exitosamente", aLong);
    }

    private CategoriaResponseDTO convertirResponse(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado(),
                categoria.getFechaCreacion(),
                categoria.getFechaModificacion()
        );
    }
}
