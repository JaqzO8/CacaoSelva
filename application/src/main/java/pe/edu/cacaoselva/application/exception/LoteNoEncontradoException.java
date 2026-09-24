package pe.edu.cacaoselva.application.exception;

public final class LoteNoEncontradoException extends RuntimeException {
    public LoteNoEncontradoException(Integer id) {
        super("No existe el lote con id " + id);
    }
}
