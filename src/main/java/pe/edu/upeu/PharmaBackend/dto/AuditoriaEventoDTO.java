package pe.edu.upeu.PharmaBackend.dto;

import java.time.LocalDateTime;
import pe.edu.upeu.PharmaBackend.entity.AuditoriaEvento;

public record AuditoriaEventoDTO(
        Long id,
        LocalDateTime fecha,
        String usuario,
        String accion,
        String entidad,
        Long entidadId,
        String detalle,
        String requestId) {

    public static AuditoriaEventoDTO de(AuditoriaEvento e) {
        return new AuditoriaEventoDTO(
                e.getId(),
                e.getFecha(),
                e.getUsuario(),
                e.getAccion(),
                e.getEntidad(),
                e.getEntidadId(),
                e.getDetalle(),
                e.getRequestId());
    }
}
