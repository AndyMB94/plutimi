package dev.andymallcco.plutimi.shared.exception;

/**
 * La operación choca con el estado actual de los datos (se traduce a 409).
 * Ejemplos: un email ya registrado, un nombre de categoría duplicado.
 */
public abstract class ConflictException extends ApplicationException {

    protected ConflictException(String message) {
        super(message);
    }

}
