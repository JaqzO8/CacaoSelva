package pe.edu.cacaoselva.application.port;

/** Puerto para verificar contraseñas con BCrypt (Fase 3). La implementación vive en infrastructure. */
public interface PasswordPort {
    /** Genera un hash BCrypt de la contraseña dada. */
    String hash(String contrasena);

    /** Verifica si la contraseña en texto plano coincide con el hash BCrypt. */
    boolean verificar(String contrasena, String hash);
}
