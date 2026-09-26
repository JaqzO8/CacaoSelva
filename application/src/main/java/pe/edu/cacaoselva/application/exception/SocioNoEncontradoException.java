package pe.edu.cacaoselva.application.exception;

public final class SocioNoEncontradoException extends RuntimeException {
    public SocioNoEncontradoException(String dni) {
        super("No existe el socio con DNI " + dni);
    }
}
