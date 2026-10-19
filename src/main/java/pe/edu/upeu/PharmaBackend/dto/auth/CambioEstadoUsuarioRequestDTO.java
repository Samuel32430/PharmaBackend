package pe.edu.upeu.PharmaBackend.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoUsuarioRequestDTO {

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;
}
