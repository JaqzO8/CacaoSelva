package pe.edu.cacaoselva.application.port;

import java.util.Optional;
import pe.edu.cacaoselva.domain.model.Usuario;

/** Puerto de lectura para usuarios (Fase 3). */
public interface UsuarioRepository {
    Optional<Usuario> findByUsuario(String usuario);
}
