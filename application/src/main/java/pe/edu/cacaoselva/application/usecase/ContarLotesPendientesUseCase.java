package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.domain.model.EstadoLote;

public final class ContarLotesPendientesUseCase {
    private final LoteQueryPort queryPort;

    public ContarLotesPendientesUseCase(LoteQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    public long execute() {
        return queryPort.findAll().stream()
                .filter(lote -> lote.estado() == EstadoLote.PENDIENTE)
                .count();
    }
}
