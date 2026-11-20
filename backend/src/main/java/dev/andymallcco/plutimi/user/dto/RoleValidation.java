package dev.andymallcco.plutimi.user.dto;

/**
 * Reglas de validación de los requests de roles.
 */
final class RoleValidation {

    /**
     * Letra inicial y después letras, dígitos o guion bajo: {@code STOCK_MANAGER}. Se guarda en
     * mayúsculas. También acepta el blanco para que un código vacío dé un solo error, el de
     * {@code @NotBlank}, y no dos.
     */
    static final String CODE_FORMAT = "\\s*|[A-Za-z][A-Za-z0-9_]*";

    private RoleValidation() {
    }

}
