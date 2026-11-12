package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDni(String dni);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            select c from Cliente c
            where c.estado = true
              and (:q is null
                   or upper(c.nombres) like upper(concat('%', :q, '%'))
                   or upper(c.apellidos) like upper(concat('%', :q, '%'))
                   or c.dni like concat('%', :q, '%'))
            """)
    Page<Cliente> buscarActivos(@Param("q") String q, Pageable pageable);
}
