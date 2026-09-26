package pe.edu.cacaoselva.application.port;

import java.util.List;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.domain.model.Lote;

/** Consulta de solo lectura; devuelve una lista sin elementos nulos, nunca null. */
public interface LoteQueryPort {
    List<Lote> findAll();

    /** Búsqueda paginada con filtros (Fase 2). Método default para no romper implementaciones existentes. */
    default PaginaLotes findAll(FiltroLotes filtro) {
        var all = findAll().stream()
                .filter(lote -> filtro.estado() == null || lote.estado() == filtro.estado())
                .filter(lote -> filtro.socioId() == null || lote.socioId().equals(filtro.socioId()))
                .toList();
        int from = Math.min(filtro.offset(), all.size());
        int to = Math.min(from + filtro.size(), all.size());
        var page = all.subList(from, to).stream().map(LoteDto::from).toList();
        int totalPages = (int) Math.ceil((double) all.size() / filtro.size());
        return new PaginaLotes(page, filtro.page(), filtro.size(), all.size(), totalPages);
    }
}
