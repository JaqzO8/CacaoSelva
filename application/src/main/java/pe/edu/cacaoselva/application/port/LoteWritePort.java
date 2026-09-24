package pe.edu.cacaoselva.application.port;

import java.util.Optional;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.Lote;

/** Las escrituras no obligan a los consumidores de solo lectura a implementar mutaciones. */
public interface LoteWritePort {
    Lote create(DatosLote datos);
    Optional<Lote> update(Integer id, DatosLote datos);
    boolean deleteById(Integer id);
}
