package dev.andymallcco.plutimi.species.dto;

/**
 * Reglas de validación compartidas por los requests de especies y razas.
 */
final class SpeciesValidation {

    /** Mayúsculas y guiones bajos, empezando por letra: DOG, GUINEA_PIG. Igual que ck_species_code_format. */
    static final String CODE_FORMAT = "[A-Z][A-Z_]*";

    /** Al menos una letra latina (con o sin tilde). {@code (?s)} hace que {@code .} incluya saltos de línea. */
    static final String NAME_HAS_LETTER = "(?s).*\\p{IsLatin}.*";

    private SpeciesValidation() {
    }

}
