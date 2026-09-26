package pe.edu.cacaoselva.application.dto;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

/** Salida de los casos de uso, independiente de su representación HTTP. */
public record LoteDto(Integer id, Integer socioId, BigDecimal pesoKg, EstadoLote estado, int version) {
    public static LoteDto from(Lote lote) {
        return new LoteDto(lote.id(), lote.socioId(), lote.pesoKg(), lote.estado(), lote.version());
    }

    public Lote toDomain() { return new Lote(id, socioId, pesoKg, estado, version); }
}
