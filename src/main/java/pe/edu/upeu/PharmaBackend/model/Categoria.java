package pe.edu.upeu.PharmaBackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "categorias")
@EntityListeners(AuditingEntityListener.class)
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String nombre;
    @Column(length = 200)
    private String descripcion;
    @Column(nullable = false)
    private Boolean estado;
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @Column(nullable = false, name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @CreatedBy
    @Column(name = "creado_por", length = 50, updatable = false)
    private String creadoPor;

    @LastModifiedBy
    @Column(name = "modificado_por", length = 50)
    private String modificadoPor;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
        if (estado == null) {
            estado = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
