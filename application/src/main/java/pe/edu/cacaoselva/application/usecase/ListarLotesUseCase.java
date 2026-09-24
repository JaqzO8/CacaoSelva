package pe.edu.cacaoselva.application.usecase;

import java.util.List;
import java.util.Objects;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.port.LoteRepository;

public final class ListarLotesUseCase {
    private final LoteRepository repository;

    public ListarLotesUseCase(LoteRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<LoteDto> execute() {
        return repository.findAll().stream().map(LoteDto::from).toList();
    }
}
