package dev.andymallcco.plutimi.shared.exception;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas HTTP con formato ProblemDetail (RFC 9457).
 * <p>
 * Hereda de {@link ResponseEntityExceptionHandler}, que ya maneja las excepciones estándar de Spring MVC
 * (ruta inexistente, método no permitido...). Sus textos en español están en {@code messages.properties}.
 * Todas las respuestas pasan por {@link #createResponseEntity}, que añade el {@code timestamp}.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_TYPE_BASE = "https://plutimi.andymallcco.dev/problems/";

    private final Clock clock;

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        return problem(ex, HttpStatus.NOT_FOUND, "resource-not-found",
                "Recurso no encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        return problem(ex, HttpStatus.CONFLICT, "conflict",
                "Conflicto con el estado actual", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        return problem(ex, HttpStatus.UNPROCESSABLE_CONTENT, "business-rule-violation",
                "Regla de negocio incumplida", ex.getMessage(), request);
    }

    /** Ordenación por una propiedad que no existe, por ejemplo {@code ?sort=unknown}. */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<Object> handleInvalidSortProperty(PropertyReferenceException ex, WebRequest request) {
        return problem(ex, HttpStatus.BAD_REQUEST, "invalid-sort", "Ordenación inválida",
                "No se puede ordenar por '" + ex.getPropertyName() + "'.", request);
    }

    /** Bloqueo optimista (@Version): otra transacción modificó la fila entre la lectura y la escritura. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> handleOptimisticLocking(OptimisticLockingFailureException ex, WebRequest request) {
        return problem(ex, HttpStatus.CONFLICT, "concurrent-modification", "Modificación concurrente",
                "El recurso fue modificado por otra operación. Vuelve a cargarlo e inténtalo de nuevo.", request);
    }

    /**
     * Última red de seguridad ante constraints de la base de datos (UNIQUE, FK...).
     * Los services validan antes; esto cubre carreras entre peticiones simultáneas.
     * El mensaje real (con nombres de constraints y SQL) solo va al log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation processing {}: {}",
                request.getDescription(false), ex.getMostSpecificCause().getMessage());
        return problem(ex, HttpStatus.CONFLICT, "data-conflict", "Conflicto de datos",
                "La operación entra en conflicto con datos existentes.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error processing {}", request.getDescription(false), ex);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.");
        body.setTitle("Error interno");
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /** {@code @Valid} sobre un {@code @RequestBody}: errores por campo y a nivel de objeto. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, validationProblem(toFieldErrors(ex.getBindingResult())),
                headers, status, request);
    }

    /**
     * Validación de parámetros del método del controller ({@code @PathVariable}, {@code @RequestParam}
     * con restricciones, o un {@code @RequestBody} validado junto a ellos).
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.isForReturnValue()) {
            // Un valor de retorno inválido es un error del servidor, no del cliente
            return super.handleHandlerMethodValidationException(ex, headers, status, request);
        }
        List<FieldErrorDetail> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                errors.addAll(toFieldErrors(parameterErrors));
            } else {
                String parameterName = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error ->
                        errors.add(new FieldErrorDetail(parameterName, error.getDefaultMessage())));
            }
        }
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            problemDetail.setProperty("timestamp", clock.instant());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> problem(Exception ex, HttpStatus status, String type, String title,
                                           String detail, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create(PROBLEM_TYPE_BASE + type));
        body.setTitle(title);
        return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
    }

    private static List<FieldErrorDetail> toFieldErrors(Errors validationErrors) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        validationErrors.getFieldErrors().forEach(error ->
                errors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
        validationErrors.getGlobalErrors().forEach(error ->
                errors.add(new FieldErrorDetail(error.getObjectName(), error.getDefaultMessage())));
        return errors;
    }

    private static ProblemDetail validationProblem(List<FieldErrorDetail> errors) {
        List<FieldErrorDetail> sortedErrors = errors.stream()
                .sorted(Comparator.comparing(FieldErrorDetail::field).thenComparing(FieldErrorDetail::message))
                .toList();
        String count = sortedErrors.size() == 1
                ? "1 error de validación"
                : sortedErrors.size() + " errores de validación";
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La petición contiene " + count);
        body.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        body.setTitle("Datos inválidos");
        body.setProperty("errors", sortedErrors);
        return body;
    }

}
