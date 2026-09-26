package pe.edu.cacaoselva.domain.exception;

/** Fase 4: concurrencia optimista — el registro fue modificado por otro usuario. */
public final class ConflictoEdicionException extends RuntimeException {
    public ConflictoEdicionException(Integer id) {
        super("El lote con id " + id + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
    }
}
