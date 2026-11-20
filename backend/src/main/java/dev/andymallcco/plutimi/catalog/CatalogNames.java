package dev.andymallcco.plutimi.catalog;

import java.util.Objects;

/**
 * Normalización de nombres compartida por las entidades del catálogo (categorías, marcas y productos).
 */
final class CatalogNames {

    private CatalogNames() {
    }

    /**
     * Nombre tal como se guardará: sin espacios al inicio o al final y con los espacios internos
     * reducidos a uno. Los services lo usan para comprobar la unicidad antes de modificar la entidad.
     */
    static String normalize(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return name.strip().replaceAll("\\s+", " ");
    }

}
