package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * Ya existe un producto con el mismo SKU o con el mismo slug.
 */
public class ProductAlreadyExistsException extends ConflictException {

    private ProductAlreadyExistsException(String message) {
        super(message);
    }

    public static ProductAlreadyExistsException withSku(String sku) {
        return new ProductAlreadyExistsException("Ya existe un producto con el SKU '" + sku + "'");
    }

    public static ProductAlreadyExistsException withSlug(String slug) {
        return new ProductAlreadyExistsException(
                "Ya existe un producto con un nombre y SKU equivalentes (identificador '" + slug + "')");
    }

}
