package pe.edu.cacaoselva.api.exception;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import pe.edu.cacaoselva.domain.exception.DatosLoteInvalidosException;
import pe.edu.cacaoselva.domain.exception.DatosSocioInvalidosException;
import pe.edu.cacaoselva.domain.exception.ConflictoEdicionException;
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import pe.edu.cacaoselva.application.exception.CredencialesInvalidasException;
import pe.edu.cacaoselva.application.exception.SocioNoEncontradoException;
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

    @ExceptionHandler(DatosSocioInvalidosException.class)
    public ResponseEntity<ApiErrorResponse> datosSocioInvalidos(DatosSocioInvalidosException error) {
        return response(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> jsonInvalido(HttpMessageNotReadableException error) {
        return response(HttpStatus.BAD_REQUEST, "JSON inválido: verifique los campos enviados.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> solicitudInvalida(IllegalArgumentException error) {
        return response(HttpStatus.BAD_REQUEST, error.getMessage() == null ? "Solicitud inválida." : error.getMessage());
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

    @ExceptionHandler(SocioNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> socioNoEncontrado(SocioNoEncontradoException error) {
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

    @ExceptionHandler(ConflictoEdicionException.class)
    public ResponseEntity<ApiErrorResponse> conflicto(ConflictoEdicionException error) {
        return response(HttpStatus.CONFLICT, error.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> credencialesInvalidas(CredencialesInvalidasException error) {
        return response(HttpStatus.UNAUTHORIZED, error.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), message, Instant.now()));
    }
}
