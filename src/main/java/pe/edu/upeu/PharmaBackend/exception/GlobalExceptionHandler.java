package pe.edu.upeu.PharmaBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.upeu.PharmaBackend.dto.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex){
         ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(ex.getMessage(), false);
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDTO);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO>reglaDeNegocio(ReglaNegocioException ex){
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(ex.getMessage(), false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarErroresDeValidacion(MethodArgumentNotValidException ex){
        String mensajeError = ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(mensajeError, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }
}
