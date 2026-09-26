package pe.edu.cacaoselva.domain.model;

/** Value object para las credenciales de autenticación. */
public record Credenciales(String usuario, String contrasena) {
    public Credenciales {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
    }
}
