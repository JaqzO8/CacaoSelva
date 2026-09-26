package pe.edu.cacaoselva.api.dto;

import java.math.BigDecimal;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.domain.model.EstadoLote;

public record GuardarLoteRequest(Integer socioId, BigDecimal pesoKg, EstadoLote estado, Integer version) {
    public GuardarLoteCommand toCommand() {
        return new GuardarLoteCommand(socioId, pesoKg, estado, version);
    }
}
