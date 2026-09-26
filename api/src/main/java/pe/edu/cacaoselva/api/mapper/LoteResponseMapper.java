package pe.edu.cacaoselva.api.mapper;

import pe.edu.cacaoselva.api.dto.LoteResponse;
import pe.edu.cacaoselva.api.dto.SocioResponse;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.dto.SocioDto;

public final class LoteResponseMapper {
    private LoteResponseMapper() {
    }

    public static LoteResponse toResponse(LoteDto lote) {
        return new LoteResponse(lote.id(), lote.socioId(), lote.pesoKg(), lote.estado().name(), lote.version());
    }

    public static SocioResponse toSocioResponse(SocioDto socio) {
        return new SocioResponse(socio.id(), socio.dni(), socio.nombre(), socio.zona(), socio.telefono());
    }
}
