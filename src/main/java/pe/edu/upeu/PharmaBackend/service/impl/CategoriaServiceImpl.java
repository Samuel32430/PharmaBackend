package pe.edu.upeu.PharmaBackend.service.impl;

import org.springframework.stereotype.Service;
import pe.edu.upeu.PharmaBackend.model.Categoria;
import pe.edu.upeu.PharmaBackend.repository.CatergoriaRepository;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;

import java.util.List;
import java.util.Optional;
@Service
public class CategoriaServiceImpl implements CategoriaService {
    private final CatergoriaRepository catergoriaRepository;

    public CategoriaServiceImpl(CatergoriaRepository catergoriaRepository) {
        this.catergoriaRepository = catergoriaRepository;
    }

    @Override
    public Categoria guardar(Categoria entidad) {
        return catergoriaRepository.save(entidad);
    }

    @Override
    public Categoria actualizar(Categoria entidad) {
        return catergoriaRepository.save(entidad);
    }

    @Override
    public Optional<Categoria> buscarPorId(Long aLong) {
        return catergoriaRepository.findById(aLong);
    }

    @Override
    public List<Categoria> listar() {
        return catergoriaRepository.findAll();
    }

    @Override
    public void eliminar(Long aLong) {
        catergoriaRepository.deleteById(aLong);
    }
}
