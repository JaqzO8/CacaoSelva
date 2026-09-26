package pe.edu.cacaoselva.application.port;

import pe.edu.cacaoselva.domain.model.Rol;

/** Puerto para generar y validar tokens JWT (Fase 3). La implementación vive en infrastructure. */
public interface TokenPort {
    /** Genera un token firmado para el usuario con el rol indicado. */
    String generar(String usuario, Rol rol);

    /** Valida el token y devuelve el nombre de usuario. Lanza excepción si el token es inválido. */
    String validarYObtenerUsuario(String token);

    /** Valida el token y devuelve el rol. Lanza excepción si el token es inválido. */
    Rol validarYObtenerRol(String token);
}
