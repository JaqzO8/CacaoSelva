package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.port.LoteRepository;

public final class BuscarLotePorIdUseCase {
    private final LoteRepository repository;

    public BuscarLotePorIdUseCase(LoteRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public LoteDto execute(Integer id) {
        ValidarIdentificadorLote.validar(id);
        return repository.findById(id)
                .map(LoteDto::from)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
    }
}
