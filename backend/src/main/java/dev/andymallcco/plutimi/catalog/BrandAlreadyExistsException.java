package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * Ya existe una marca con el mismo nombre (sin distinguir mayúsculas) o con el mismo slug.
 */
public class BrandAlreadyExistsException extends ConflictException {

    private BrandAlreadyExistsException(String message) {
        super(message);
    }

    public static BrandAlreadyExistsException withName(String name) {
        return new BrandAlreadyExistsException("Ya existe una marca con el nombre '" + name + "'");
    }

    public static BrandAlreadyExistsException withSlug(String slug) {
        return new BrandAlreadyExistsException(
                "Ya existe una marca con un nombre equivalente (identificador '" + slug + "')");
    }

}
