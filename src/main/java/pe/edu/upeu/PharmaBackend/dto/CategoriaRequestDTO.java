package pe.edu.upeu.PharmaBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CategoriaRequestDTO {
    @NotBlank(message = "El nombre de la categoria es obligatoria")
    @Size(min=3,max=50,message = "El nombre de la categoria debe tener entre 3 y 50 caracteres")
    private String nombre;
    @Size(max=200,message = "La descripcion no debe superar los 200 caracteres")
    private String descripcion;
    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;
}
