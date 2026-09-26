package pe.edu.cacaoselva.application.usecase;

import java.util.List;
import java.util.Objects;
import pe.edu.cacaoselva.application.dto.SocioDto;
import pe.edu.cacaoselva.application.port.SocioRepository;

public final class ListarSociosUseCase {
    private final SocioRepository repository;

    public ListarSociosUseCase(SocioRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<SocioDto> execute() {
        return repository.findAll().stream().map(SocioDto::from).toList();
    }
}
