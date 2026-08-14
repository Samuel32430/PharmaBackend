package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.PharmaBackend.model.Categoria;

public interface CatergoriaRepository extends JpaRepository<Categoria, Long> {
}
