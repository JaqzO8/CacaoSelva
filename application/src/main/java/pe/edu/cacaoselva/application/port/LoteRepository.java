package pe.edu.cacaoselva.application.port;

import java.util.Optional;
import pe.edu.cacaoselva.domain.model.Lote;

/** Agrega búsqueda por identidad al contrato mínimo de consulta. */
public interface LoteRepository extends LoteQueryPort {
    Optional<Lote> findById(Integer id);
}
