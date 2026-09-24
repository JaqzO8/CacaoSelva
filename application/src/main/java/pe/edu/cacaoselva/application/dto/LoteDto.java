package pe.edu.cacaoselva.application.dto;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

/** Salida de los casos de uso, independiente de su representación HTTP. */
public record LoteDto(Integer id, String socio, BigDecimal pesoKg, EstadoLote estado) {
    public static LoteDto from(Lote lote) {
        return new LoteDto(lote.id(), lote.socio(), lote.pesoKg(), lote.estado());
    }
}
