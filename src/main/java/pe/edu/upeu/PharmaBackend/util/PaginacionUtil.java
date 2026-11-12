package pe.edu.upeu.PharmaBackend.util;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;

/**
 * Utilidad compartida para validar parámetros de paginación y ordenamiento
 * contra tamaño máximo (100) y lista blanca de campos permitidos.
 */
public final class PaginacionUtil {

    public static final int TAMANIO_MINIMO = 1;
    public static final int TAMANIO_MAXIMO = 100;

    private PaginacionUtil() {
    }

    public static Pageable construir(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion,
            Set<String> camposOrdenables,
            String campoPorDefecto) {

        if (pagina < 0) {
            throw new ReglaNegocioException("El número de página no puede ser negativo");
        }
        if (tamanio < TAMANIO_MINIMO || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException(
                    "El tamaño de página debe estar entre " + TAMANIO_MINIMO + " y " + TAMANIO_MAXIMO);
        }

        String campo = (ordenarPor == null || ordenarPor.isBlank())
                ? campoPorDefecto
                : ordenarPor.trim();

        if (!camposOrdenables.contains(campo)) {
            throw new ReglaNegocioException(
                    "El campo de ordenamiento '" + campo + "' no está permitido. Campos válidos: "
                            + camposOrdenables);
        }

        String sentido = (direccion == null || direccion.isBlank()) ? "asc" : direccion.trim();
        if (!"asc".equalsIgnoreCase(sentido) && !"desc".equalsIgnoreCase(sentido)) {
            throw new ReglaNegocioException(
                    "La dirección de ordenamiento '" + sentido + "' no está permitida. Valores válidos: asc, desc");
        }

        Sort.Direction dir = "desc".equalsIgnoreCase(sentido) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(pagina, tamanio, Sort.by(dir, campo));
    }
}
