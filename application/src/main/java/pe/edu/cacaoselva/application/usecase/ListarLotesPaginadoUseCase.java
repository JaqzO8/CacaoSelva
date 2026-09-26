package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.application.port.LoteQueryPort;

/** Fase 2: lista lotes con paginación y filtros. */
public final class ListarLotesPaginadoUseCase {
    private final LoteQueryPort queryPort;

    public ListarLotesPaginadoUseCase(LoteQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    public PaginaLotes execute(FiltroLotes filtro) {
        Objects.requireNonNull(filtro, "El filtro es obligatorio.");
        return queryPort.findAll(filtro);
    }
}
