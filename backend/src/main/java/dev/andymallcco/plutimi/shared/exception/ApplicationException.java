package dev.andymallcco.plutimi.shared.exception;

/**
 * Raíz de las excepciones de Plutimi.
 * El mensaje está dirigido al usuario final (en español) y se usa como {@code detail} del ProblemDetail.
 * Las excepciones no conocen HTTP: la traducción a códigos de estado la hace GlobalExceptionHandler.
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

}
