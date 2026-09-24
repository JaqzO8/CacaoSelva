package pe.edu.cacaoselva.infrastructure.http;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

record HttpLoteResponse(Integer id, String socio, BigDecimal pesoKg, String estado) {
    Lote toDomain() {
        if (estado == null) {
            throw new IllegalArgumentException("La API no proporcionó el estado del lote.");
        }
        return new Lote(id, socio, pesoKg, EstadoLote.valueOf(estado));
    }
}
