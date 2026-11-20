package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * No se puede desactivar una categoría que tiene productos a la venta (409): quedarían productos
 * activos en una categoría oculta. Primero hay que desactivarlos o moverlos a otra categoría.
 */
public class CategoryHasActiveProductsException extends ConflictException {

    public CategoryHasActiveProductsException(String categoryName) {
        super("La categoría '" + categoryName + "' tiene productos activos: desactívalos o muévelos a otra categoría antes");
    }

}
