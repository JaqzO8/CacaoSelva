package pe.edu.cacaoselva.api.mapper;

import pe.edu.cacaoselva.api.dto.LoteResponse;
import pe.edu.cacaoselva.application.dto.LoteDto;

public final class LoteResponseMapper {
    private LoteResponseMapper() {
    }

    public static LoteResponse toResponse(LoteDto lote) {
        return new LoteResponse(lote.id(), lote.socio(), lote.pesoKg(), lote.estado().name());
    }
}
