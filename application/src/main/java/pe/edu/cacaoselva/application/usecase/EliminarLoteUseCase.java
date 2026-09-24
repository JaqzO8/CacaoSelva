package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.port.LoteWritePort;

public final class EliminarLoteUseCase {
    private final LoteWritePort writer;

    public EliminarLoteUseCase(LoteWritePort writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    public void execute(Integer id) {
        ValidarIdentificadorLote.validar(id);
        if (!writer.deleteById(id)) {
            throw new LoteNoEncontradoException(id);
        }
    }
}
