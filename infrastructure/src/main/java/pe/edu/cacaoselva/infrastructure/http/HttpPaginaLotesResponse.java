package pe.edu.cacaoselva.infrastructure.http;

import java.util.List;

record HttpPaginaLotesResponse(List<HttpLoteResponse> content, int page, int size,
                              long totalElements, int totalPages) { }
