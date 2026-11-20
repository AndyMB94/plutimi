package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.BusinessException;

/**
 * Se intenta retirar más unidades de las que hay en stock (422).
 */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int available, int requested) {
        super("Stock insuficiente para '" + productName + "': hay " + available
                + " unidades y se solicitaron " + requested);
    }

}
