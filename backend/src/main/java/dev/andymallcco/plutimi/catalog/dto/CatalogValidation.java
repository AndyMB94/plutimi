package dev.andymallcco.plutimi.catalog.dto;

/**
 * Reglas de validación compartidas por los requests del catálogo (categorías, marcas y productos).
 */
final class CatalogValidation {

    /**
     * Al menos una letra latina (con o sin tilde) o un dígito, para que siempre se pueda generar un slug.
     * {@code (?s)} hace que {@code .} incluya saltos de línea.
     */
    static final String NAME_HAS_LETTER_OR_DIGIT = "(?s).*[\\p{IsLatin}0-9].*";

    private CatalogValidation() {
    }

}
