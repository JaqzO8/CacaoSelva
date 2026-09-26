package pe.edu.cacaoselva.application.dto;

import pe.edu.cacaoselva.domain.model.EstadoLote;

/** Filtro para búsqueda avanzada de lotes (Fase 2). */
public record FiltroLotes(EstadoLote estado, Integer socioId, String socio, int page, int size) {
    public FiltroLotes(EstadoLote estado, Integer socioId, int page, int size) {
        this(estado, socioId, null, page, size);
    }

    public FiltroLotes {
        if (page < 0) {
            throw new IllegalArgumentException("La página debe ser mayor o igual a 0.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100.");
        }
        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("La página solicitada excede el rango permitido.");
        }
        if (socio != null) {
            socio = socio.strip();
            if (socio.isEmpty()) socio = null;
            else if (socio.length() > 120) throw new IllegalArgumentException("El filtro de socio admite hasta 120 caracteres.");
        }
        if (socioId != null && socioId <= 0) throw new IllegalArgumentException("El ID del socio debe ser positivo.");
    }

    public int offset() {
        return page * size;
    }
}
