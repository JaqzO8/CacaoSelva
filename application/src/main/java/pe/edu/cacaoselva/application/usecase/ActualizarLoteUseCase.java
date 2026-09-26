package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.application.port.AuditoriaPort;
import java.util.Locale;

public final class ActualizarLoteUseCase {
    private final LoteWritePort writer;
    private final AuditoriaPort auditoria;

    public ActualizarLoteUseCase(LoteWritePort writer) {
        this(writer, null);
    }

    public ActualizarLoteUseCase(LoteWritePort writer, AuditoriaPort auditoria) {
        this.writer = Objects.requireNonNull(writer);
        this.auditoria = auditoria;
    }

    public LoteDto execute(Integer id, GuardarLoteCommand command) {
        return execute(id, command, null);
    }

    public LoteDto execute(Integer id, GuardarLoteCommand command, String usuario) {
        ValidarIdentificadorLote.validar(id);
        var lote = writer.update(id, command.toDomain(), command.version()).map(LoteDto::from)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
        if (auditoria != null) {
            String datos = String.format(Locale.ROOT, "{\"socioId\":%d,\"pesoKg\":%s,\"estado\":\"%s\",\"version\":%d}",
                    lote.socioId(), lote.pesoKg().toPlainString(), lote.estado().name(), lote.version());
            auditoria.registrar(lote.id(), "ACTUALIZAR", usuario, datos);
        }
        return lote;
    }
}
