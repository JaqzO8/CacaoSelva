package pe.edu.cacaoselva.application.exception;

/** Fallo del puerto remoto, sin exponer tipos HTTP o JSON a sus consumidores. */
public final class ApiNoDisponibleException extends RuntimeException {
    public ApiNoDisponibleException(String message) {
        super(message);
    }

    public ApiNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
