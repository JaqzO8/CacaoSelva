package pe.edu.cacaoselva.domain.model;

import java.math.BigDecimal;

public record Lote(Integer id, Integer socioId, BigDecimal pesoKg, EstadoLote estado, int version) {
    public Lote {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del lote debe ser mayor que cero.");
        }
        DatosLote datos = new DatosLote(socioId, pesoKg, estado);
        socioId = datos.socioId();
        if (version < 1) {
            throw new IllegalArgumentException("La versión del lote debe ser al menos 1.");
        }
    }

    /** Constructor de conveniencia para lotes nuevos con versión inicial. */
    public Lote(Integer id, Integer socioId, BigDecimal pesoKg, EstadoLote estado) {
        this(id, socioId, pesoKg, estado, 1);
    }
}
