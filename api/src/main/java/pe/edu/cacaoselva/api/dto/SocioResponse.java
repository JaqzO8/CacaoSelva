package pe.edu.cacaoselva.api.dto;

import java.math.BigDecimal;

/** Respuesta HTTP para socios (Fase 1). */
public record SocioResponse(Integer id, String dni, String nombre, String zona, String telefono) {
}
