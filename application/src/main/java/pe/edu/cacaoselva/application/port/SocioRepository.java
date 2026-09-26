package pe.edu.cacaoselva.application.port;

import java.util.List;
import java.util.Optional;
import pe.edu.cacaoselva.domain.model.Socio;

/** Puerto de lectura para socios. */
public interface SocioRepository {
    List<Socio> findAll();
    Optional<Socio> findById(Integer id);
    Optional<Socio> findByDni(String dni);
}
