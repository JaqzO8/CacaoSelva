package pe.edu.cacaoselva.api.exception;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import pe.edu.cacaoselva.domain.exception.DatosLoteInvalidosException;
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.edu.cacaoselva.api.dto.ApiErrorResponse;
import pe.edu.cacaoselva.application.exception.IdentificadorLoteInvalidoException;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DatosLoteInvalidosException.class)
    public ResponseEntity<ApiErrorResponse> datosInvalidos(DatosLoteInvalidosException error) {
        return response(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> jsonInvalido(HttpMessageNotReadableException error) {
        return response(HttpStatus.BAD_REQUEST, "JSON inválido: indique socio, pesoKg numérico y estado PENDIENTE o LIQUIDADO.");
    }

    @ExceptionHandler(PersistenciaLoteException.class)
    public ResponseEntity<ApiErrorResponse> persistencia(PersistenciaLoteException error) {
        LOGGER.error("Fallo de persistencia de lotes", error);
        return response(HttpStatus.SERVICE_UNAVAILABLE, error.getMessage());
    }
    @ExceptionHandler(LoteNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> noEncontrado(LoteNoEncontradoException error) {
        return response(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(IdentificadorLoteInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> identificadorInvalido(IdentificadorLoteInvalidoException error) {
        return response(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException error) {
        return response(HttpStatus.BAD_REQUEST, new IdentificadorLoteInvalidoException().getMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), message, Instant.now()));
    }
}
