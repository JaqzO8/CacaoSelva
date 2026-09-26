package pe.edu.cacaoselva.application.dto;

import java.util.List;

/** Respuesta paginada para lotes (Fase 2). */
public record PaginaLotes(List<LoteDto> content, int page, int size, long totalElements, int totalPages) {
}
