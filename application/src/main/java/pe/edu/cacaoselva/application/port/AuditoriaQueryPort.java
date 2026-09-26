package pe.edu.cacaoselva.application.port;

import java.util.List;
import pe.edu.cacaoselva.application.dto.EventoAuditoria;

public interface AuditoriaQueryPort {
    List<EventoAuditoria> listarPorLote(Integer loteId);
}
