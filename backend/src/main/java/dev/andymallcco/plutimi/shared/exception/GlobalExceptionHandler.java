package dev.andymallcco.plutimi.shared.exception;

import java.net.URI;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
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
        return handleApplicationException(ex, HttpStatus.NOT_FOUND,
                "resource-not-found", "Recurso no encontrado", request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        return handleApplicationException(ex, HttpStatus.CONFLICT,
                "conflict", "Conflicto con el estado actual", request);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        return handleApplicationException(ex, HttpStatus.UNPROCESSABLE_CONTENT,
                "business-rule-violation", "Regla de negocio incumplida", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error processing {}", request.getDescription(false), ex);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.");
        body.setTitle("Error interno");
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            problemDetail.setProperty("timestamp", clock.instant());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> handleApplicationException(
            ApplicationException ex, HttpStatus status, String type, String title, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        body.setType(URI.create(PROBLEM_TYPE_BASE + type));
        body.setTitle(title);
        return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
    }

}
