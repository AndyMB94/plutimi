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

    /** Letras ASCII y dígitos en bloques separados por un guion: {@code RC-AD-15}. Se guarda en mayúsculas. */
    static final String SKU_FORMAT = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$";

    /** URL absoluta http(s) sin espacios. La subida de imágenes llega en la fase 4. */
    static final String HTTP_URL = "^https?://\\S+$";

    static final int MAX_STOCK_DELTA = 1_000_000;

    private CatalogValidation() {
    }

}
