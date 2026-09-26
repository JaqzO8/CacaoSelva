package pe.edu.cacaoselva.api.dto;

/** Petición de autenticación (Fase 3). */
public record LoginRequest(String usuario, String contrasena) {
}
