package pe.edu.cacaoselva.application.exception;

public final class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos.");
    }
}
