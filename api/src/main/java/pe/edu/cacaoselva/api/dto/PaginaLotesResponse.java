package pe.edu.cacaoselva.api.dto;

import java.util.List;

/** Respuesta paginada HTTP para lotes (Fase 2). */
public record PaginaLotesResponse(List<LoteResponse> content, int page, int size, long totalElements, int totalPages) {
}
