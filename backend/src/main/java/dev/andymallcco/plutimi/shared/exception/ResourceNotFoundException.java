package dev.andymallcco.plutimi.shared.exception;

/**
 * El recurso pedido no existe o no pertenece al usuario actual (se traduce a 404).
 * Ejemplo de subclase: {@code CategoryNotFoundException}.
 */
public abstract class ResourceNotFoundException extends ApplicationException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }

}
