package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.application.port.AuditoriaPort;
import java.util.Locale;

public final class CrearLoteUseCase {
    private final LoteWritePort writer;
    private final AuditoriaPort auditoria;

    public CrearLoteUseCase(LoteWritePort writer) {
        this(writer, null);
    }

    public CrearLoteUseCase(LoteWritePort writer, AuditoriaPort auditoria) {
        this.writer = Objects.requireNonNull(writer);
        this.auditoria = auditoria;
    }

    public LoteDto execute(GuardarLoteCommand command) {
        return execute(command, null);
    }

    public LoteDto execute(GuardarLoteCommand command, String usuario) {
        var lote = LoteDto.from(writer.create(command.toDomain()));
        if (auditoria != null) auditoria.registrar(lote.id(), "CREAR", usuario, datos(lote));
        return lote;
    }

    private String datos(LoteDto lote) {
        return String.format(Locale.ROOT, "{\"socioId\":%d,\"pesoKg\":%s,\"estado\":\"%s\",\"version\":%d}",
                lote.socioId(), lote.pesoKg().toPlainString(), lote.estado().name(), lote.version());
    }
}
