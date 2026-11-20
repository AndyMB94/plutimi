package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.BusinessException;

/** No se puede asignar una marca desactivada a un producto (422). */
public class InactiveBrandException extends BusinessException {

    public InactiveBrandException(String brandName) {
        super("La marca '" + brandName + "' está desactivada");
    }

}
