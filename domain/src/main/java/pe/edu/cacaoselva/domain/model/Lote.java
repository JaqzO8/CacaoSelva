package pe.edu.cacaoselva.domain.model;

import java.math.BigDecimal;

public record Lote(Integer id, String socio, BigDecimal pesoKg, EstadoLote estado) {
    public Lote {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del lote debe ser mayor que cero.");
        }
        DatosLote datos = new DatosLote(socio, pesoKg, estado);
        socio = datos.socio();
    }
}
