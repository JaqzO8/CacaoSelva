package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.application.port.AuditoriaPort;
import pe.edu.cacaoselva.application.dto.LoteDto;

public final class EliminarLoteUseCase {
    private final LoteWritePort writer;
    private final LoteRepository reader;
    private final AuditoriaPort auditoria;

    public EliminarLoteUseCase(LoteWritePort writer) {
        this(writer, null, null);
    }

    public EliminarLoteUseCase(LoteWritePort writer, LoteRepository reader, AuditoriaPort auditoria) {
        this.writer = Objects.requireNonNull(writer);
        this.reader = reader;
        this.auditoria = auditoria;
    }

    public void execute(Integer id) {
        execute(id, null);
    }

    public void execute(Integer id, String usuario) {
        ValidarIdentificadorLote.validar(id);
        var lote = reader == null ? null : reader.findById(id).orElse(null);
        if (!writer.deleteById(id)) {
            throw new LoteNoEncontradoException(id);
        }
        if (auditoria != null && lote != null) {
            var dto = LoteDto.from(lote);
            String datos = String.format(java.util.Locale.ROOT,
                    "{\"socioId\":%d,\"pesoKg\":%s,\"estado\":\"%s\",\"version\":%d}",
                    dto.socioId(), dto.pesoKg().toPlainString(), dto.estado().name(), dto.version());
            auditoria.registrar(id, "ELIMINAR", usuario, datos);
        }
    }
}
