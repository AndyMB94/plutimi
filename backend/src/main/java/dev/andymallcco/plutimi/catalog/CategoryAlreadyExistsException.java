package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * Ya existe una categoría con el mismo nombre (sin distinguir mayúsculas) o con el mismo slug.
 * Se crea con los métodos de fábrica para que el mensaje explique cuál de los dos casos ocurrió.
 */
public class CategoryAlreadyExistsException extends ConflictException {

    private CategoryAlreadyExistsException(String message) {
        super(message);
    }

    public static CategoryAlreadyExistsException withName(String name) {
        return new CategoryAlreadyExistsException("Ya existe una categoría con el nombre '" + name + "'");
    }

    public static CategoryAlreadyExistsException withSlug(String slug) {
        return new CategoryAlreadyExistsException(
                "Ya existe una categoría con un nombre equivalente (identificador '" + slug + "')");
    }

}
