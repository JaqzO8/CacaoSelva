package pe.edu.cacaoselva.application.dto;

import java.time.Instant;

public record EventoAuditoria(long id, Integer loteId, String accion, String usuario,
                              String datosJson, Instant fecha) { }
