package pe.edu.cacaoselva.application.exception;

public final class IdentificadorLoteInvalidoException extends RuntimeException {
    public IdentificadorLoteInvalidoException() {
        super("El identificador del lote debe ser un entero mayor que cero.");
    }
}
