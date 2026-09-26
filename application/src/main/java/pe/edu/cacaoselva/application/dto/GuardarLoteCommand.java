package pe.edu.cacaoselva.application.dto;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.EstadoLote;

public record GuardarLoteCommand(Integer socioId, BigDecimal pesoKg, EstadoLote estado, Integer version) {
    /** Constructor sin versión para operaciones de creación. */
    public GuardarLoteCommand(Integer socioId, BigDecimal pesoKg, EstadoLote estado) {
        this(socioId, pesoKg, estado, null);
    }

    public DatosLote toDomain() {
        return new DatosLote(socioId, pesoKg, estado);
    }
}
