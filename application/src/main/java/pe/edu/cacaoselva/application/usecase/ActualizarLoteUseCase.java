package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.port.LoteWritePort;

public final class ActualizarLoteUseCase {
    private final LoteWritePort writer;

    public ActualizarLoteUseCase(LoteWritePort writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    public LoteDto execute(Integer id, GuardarLoteCommand command) {
        ValidarIdentificadorLote.validar(id);
        return writer.update(id, command.toDomain()).map(LoteDto::from)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
    }
}
