package pe.edu.cacaoselva.application.port;

import pe.edu.cacaoselva.domain.model.Rol;
import pe.edu.cacaoselva.domain.model.Usuario;

public interface UsuarioWritePort {
    Usuario create(String usuario, String passwordHash, Rol rol);
}
