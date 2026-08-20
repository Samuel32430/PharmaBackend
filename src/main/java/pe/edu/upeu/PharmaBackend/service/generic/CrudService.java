package pe.edu.upeu.PharmaBackend.service.generic;

import java.util.List;

public interface CrudService<REQ,RES, ID> {
    RES guardar(REQ t);
    RES actualizar(ID id, REQ t);
    RES buscarPorId(ID id);
    List<RES> listar();
    void eliminar(ID id);
}
