package pe.edu.cacaoselva.domain.model;

import java.math.BigDecimal;
import pe.edu.cacaoselva.domain.exception.DatosLoteInvalidosException;

/** Datos de negocio sin identidad; se usan tanto al registrar como al modificar. */
public record DatosLote(Integer socioId, BigDecimal pesoKg, EstadoLote estado) {
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("999999999.999");

    public DatosLote {
        if (socioId == null || socioId <= 0) {
            throw new DatosLoteInvalidosException("El socio es obligatorio (socioId mayor que cero).");
        }
        if (pesoKg == null || pesoKg.signum() <= 0 || pesoKg.compareTo(PESO_MAXIMO) > 0
                || pesoKg.stripTrailingZeros().scale() > 3) {
            throw new DatosLoteInvalidosException("El peso debe ser positivo, hasta 999999999.999 kg y con máximo 3 decimales.");
        }
        if (estado == null) {
            throw new DatosLoteInvalidosException("El estado es obligatorio.");
        }
    }
}
