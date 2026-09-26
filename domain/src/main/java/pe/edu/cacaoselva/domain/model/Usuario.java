package pe.edu.cacaoselva.domain.model;

/** Entidad de dominio que representa un usuario del sistema. */
public record Usuario(Integer id, String usuario, String hash, Rol rol, boolean activo) {
    public Usuario {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser mayor que cero.");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("El hash de contraseña es obligatorio.");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
    }
}
