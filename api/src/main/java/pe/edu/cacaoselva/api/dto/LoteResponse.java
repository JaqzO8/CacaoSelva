package pe.edu.cacaoselva.api.dto;

import java.math.BigDecimal;

public record LoteResponse(Integer id, Integer socioId, BigDecimal pesoKg, String estado, int version) {
}
