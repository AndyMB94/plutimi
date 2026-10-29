package dev.andymallcco.plutimi.shared.exception;

/**
 * Los datos son válidos, pero la operación viola una regla de negocio (se traduce a 422).
 * Ejemplos: stock insuficiente, cancelar un pedido ya enviado.
 */
public abstract class BusinessException extends ApplicationException {

    protected BusinessException(String message) {
        super(message);
    }

}
