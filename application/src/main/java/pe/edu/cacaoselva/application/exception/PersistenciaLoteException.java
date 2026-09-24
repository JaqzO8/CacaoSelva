package pe.edu.cacaoselva.application.exception;

public final class PersistenciaLoteException extends RuntimeException {
    public PersistenciaLoteException(Throwable cause) {
        super("No se pudo acceder al almacenamiento de lotes.", cause);
    }
}
