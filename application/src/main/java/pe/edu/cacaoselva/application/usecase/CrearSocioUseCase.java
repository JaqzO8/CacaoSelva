package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.GuardarSocioCommand;
import pe.edu.cacaoselva.application.dto.SocioDto;
import pe.edu.cacaoselva.application.port.SocioWritePort;

public final class CrearSocioUseCase {
    private final SocioWritePort writer;

    public CrearSocioUseCase(SocioWritePort writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    public SocioDto execute(GuardarSocioCommand command) {
        return SocioDto.from(writer.create(command.toDomain()));
    }
}
