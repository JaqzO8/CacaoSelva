package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.SocioDto;
import pe.edu.cacaoselva.application.exception.SocioNoEncontradoException;
import pe.edu.cacaoselva.application.port.SocioRepository;

public final class BuscarSocioPorDniUseCase {
    private final SocioRepository repository;

    public BuscarSocioPorDniUseCase(SocioRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public SocioDto execute(String dni) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("El DNI es obligatorio.");
        }
        return repository.findByDni(dni.strip())
                .map(SocioDto::from)
                .orElseThrow(() -> new SocioNoEncontradoException(dni));
    }
}
