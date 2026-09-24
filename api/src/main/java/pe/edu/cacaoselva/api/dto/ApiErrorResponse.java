package pe.edu.cacaoselva.api.dto;

import java.time.Instant;

public record ApiErrorResponse(int status, String message, Instant timestamp) {
}
