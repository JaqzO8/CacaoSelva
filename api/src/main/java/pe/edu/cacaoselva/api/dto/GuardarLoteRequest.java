package pe.edu.cacaoselva.api.dto;

import java.math.BigDecimal;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.domain.model.EstadoLote;

public record GuardarLoteRequest(String socio, BigDecimal pesoKg, EstadoLote estado) {
    public GuardarLoteCommand toCommand() {
        return new GuardarLoteCommand(socio, pesoKg, estado);
    }
}
