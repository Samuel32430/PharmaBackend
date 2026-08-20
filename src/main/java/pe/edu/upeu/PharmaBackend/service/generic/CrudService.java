package pe.edu.upeu.PharmaBackend.service.generic;

import java.util.List;
import java.util.Optional;

public interface CrudService<REQ,RES, ID> {
    REQ guardar(REQ t);
    REQ actualizar(ID id, REQ t);
    Optional<RES> buscarPorId(ID id);
    List<RES> listar();
    void eliminar(ID id);
}
