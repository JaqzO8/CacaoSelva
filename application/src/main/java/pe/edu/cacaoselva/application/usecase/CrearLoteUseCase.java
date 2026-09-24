package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.port.LoteWritePort;

public final class CrearLoteUseCase {
    private final LoteWritePort writer;

    public CrearLoteUseCase(LoteWritePort writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    public LoteDto execute(GuardarLoteCommand command) {
        return LoteDto.from(writer.create(command.toDomain()));
    }
}
