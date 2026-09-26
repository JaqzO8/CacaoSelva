package pe.edu.cacaoselva.infrastructure.http;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

record HttpLoteResponse(Integer id, Integer socioId, BigDecimal pesoKg, String estado, int version) {
    Lote toDomain() {
        if (estado == null) {
            throw new IllegalArgumentException("La API no proporcionó el estado del lote.");
        }
        return new Lote(id, socioId, pesoKg, EstadoLote.valueOf(estado), version);
    }
}
