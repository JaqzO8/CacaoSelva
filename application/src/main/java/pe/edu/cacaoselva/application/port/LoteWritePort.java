package pe.edu.cacaoselva.application.port;

import java.util.Optional;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.Lote;

/** Las escrituras no obligan a los consumidores de solo lectura a implementar mutaciones. */
public interface LoteWritePort {
    Lote create(DatosLote datos);

    /**
     * Actualiza un lote con control de concurrencia optimista (Fase 4).
     * @param id ID del lote
     * @param datos nuevos datos
     * @param version versión esperada; los clientes HTTP deben enviarla al actualizar
     * @return el lote actualizado, o vacío si no existe
     * @throws pe.edu.cacaoselva.domain.exception.ConflictoEdicionException si la versión no coincide
     */
    Optional<Lote> update(Integer id, DatosLote datos, Integer version);

    /** Mantiene compatibilidad con el contrato original sin versión. */
    default Optional<Lote> update(Integer id, DatosLote datos) {
        return update(id, datos, null);
    }

    boolean deleteById(Integer id);
}
