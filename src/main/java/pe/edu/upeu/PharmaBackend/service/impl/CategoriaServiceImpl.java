package pe.edu.upeu.PharmaBackend.service.impl;

import org.springframework.stereotype.Service;
import pe.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.model.Categoria;
import pe.edu.upeu.PharmaBackend.repository.CatergoriaRepository;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;
import org.slf4j.Logger;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiceImpl implements CategoriaService {


    private static final Logger LOG = Logger.getLogger(CategoriaServiceImpl.class);

    private final CatergoriaRepository catergoriaRepository;

    public CategoriaServiceImpl(CatergoriaRepository catergoriaRepository) {
        this.catergoriaRepository = catergoriaRepository;
    }

    @Override
    public CategoriaRequestDTO guardar(CategoriaRequestDTO t) {
        return null;
    }

    @Override
    public CategoriaRequestDTO actualizar(Long aLong, CategoriaRequestDTO t) {
        return null;
    }

    @Override
    public Optional<CategoriaResponseDTO> buscarPorId(Long aLong) {
        return Optional.empty();
    }

    @Override
    public List<CategoriaResponseDTO> listar() {
        return List.of();
    }

    @Override
    public void eliminar(Long aLong) {

    }
}
