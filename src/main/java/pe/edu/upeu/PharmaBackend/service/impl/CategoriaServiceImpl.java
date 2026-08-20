package pe.edu.upeu.PharmaBackend.service.impl;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import pe.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.model.Categoria;
import pe.edu.upeu.PharmaBackend.repository.CatergoriaRepository;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;
import org.slf4j.Logger;
import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {


    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CategoriaServiceImpl.class);

    private final CatergoriaRepository catergoriaRepository;

    public CategoriaServiceImpl(CatergoriaRepository catergoriaRepository) {
        this.catergoriaRepository = catergoriaRepository;
    }

    @Override
    @Transactional
    public CategoriaResponseDTO guardar(CategoriaRequestDTO t) {
        String nombre = t.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new ReglaNegocioException("El nombre de la categoria no puede estar vacio");
        }
        if (catergoriaRepository.existsByNombreIgnoreCase(nombre)){
            throw new ReglaNegocioException(
                    "Ya existe una categoria con el nombre " + nombre
            );
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(t.getDescripcion());
        categoria.setEstado(t.getEstado());

        Categoria catCreada = catergoriaRepository.save(categoria);
        return convertirResponse(catCreada);
    }

    @Override
    @Transactional
    public CategoriaResponseDTO actualizar(Long aLong, CategoriaRequestDTO t) {
        Categoria categoria = catergoriaRepository.findById(aLong).orElseThrow(()->
                new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: "+ aLong
                )
        );
        
        String nombre = t.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new ReglaNegocioException("El nombre de la categoria no puede estar vacio");
        }
        if (catergoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, aLong)){
            throw new ReglaNegocioException(
                    "Ya existe otra categoria con el nombre " + nombre
            );
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(t.getDescripcion());
        categoria.setEstado(t.getEstado());

        Categoria catActualizada = catergoriaRepository.save(categoria);
        return convertirResponse(catActualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long aLong) {
        Categoria categoria = catergoriaRepository.findById(aLong).orElseThrow(()->
                new RecursoNoEncontradoException(
                        "Categoria no encontrada con id"+aLong
                )
        );
        return convertirResponse(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar() {
        return catergoriaRepository.findAll()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    public void eliminar(Long aLong) {
        Categoria categoria = catergoriaRepository.findById(aLong).orElseThrow(()->
                new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: "+ aLong
                )
        );
        catergoriaRepository.delete(categoria);
    }

    private CategoriaResponseDTO convertirResponse(Categoria categoria){
        return   new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado(),
                categoria.getFechaCreacion(),
                categoria.getFechaModificacion()
        );
    }
}
