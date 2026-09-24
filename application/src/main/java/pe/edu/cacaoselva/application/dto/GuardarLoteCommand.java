package pe.edu.cacaoselva.application.dto;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.EstadoLote;

public record GuardarLoteCommand(String socio, BigDecimal pesoKg, EstadoLote estado) {
    public DatosLote toDomain() {
        return new DatosLote(socio, pesoKg, estado);
    }
}
