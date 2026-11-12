package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    boolean existsByCategoriaId(Long categoriaId);

    @Override
    @EntityGraph(attributePaths = {"categoria"})
    Page<Producto> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"categoria"})
    @Query("""
            select p from Producto p
            where p.estado = true
              and p.stock > 0
              and (:q is null
                   or upper(p.nombre) like upper(concat('%', :q, '%'))
                   or upper(p.categoria.nombre) like upper(concat('%', :q, '%')))
            """)
    Page<Producto> buscarDisponibles(@Param("q") String q, Pageable pageable);
}
