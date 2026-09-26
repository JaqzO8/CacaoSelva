package pe.edu.cacaoselva.application.port;

/** Puerto de auditoría (Fase 4). Registra las operaciones sobre lotes. */
public interface AuditoriaPort {
    void registrar(Integer loteId, String accion, String usuario, String datosJson);
}
