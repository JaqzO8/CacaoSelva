package pe.edu.cacaoselva.application.exception;

public final class SocioNoEncontradoException extends RuntimeException {
    public SocioNoEncontradoException(Integer id) {
        super("No existe el socio con ID " + id);
    }
    public SocioNoEncontradoException(String dni) {
        super("No existe el socio con DNI " + dni);
    }
}
