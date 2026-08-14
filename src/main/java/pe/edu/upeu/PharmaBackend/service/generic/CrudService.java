package pe.edu.upeu.PharmaBackend.service.generic;

import java.util.List;
import java.util.Optional;

public interface CrudService<T, ID> {
    T guardar(T entidad);
    T actualizar(T entidad);
    Optional<T> buscarPorId(ID id);
    List<T> listar();
    void eliminar(ID id);
}
