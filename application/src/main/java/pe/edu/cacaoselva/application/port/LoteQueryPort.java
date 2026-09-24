package pe.edu.cacaoselva.application.port;

import java.util.List;
import pe.edu.cacaoselva.domain.model.Lote;

/** Consulta de solo lectura; devuelve una lista sin elementos nulos, nunca null. */
@FunctionalInterface
public interface LoteQueryPort {
    List<Lote> findAll();
}
