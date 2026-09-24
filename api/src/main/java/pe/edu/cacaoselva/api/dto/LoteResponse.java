package pe.edu.cacaoselva.api.dto;

import java.math.BigDecimal;

public record LoteResponse(Integer id, String socio, BigDecimal pesoKg, String estado) {
}
