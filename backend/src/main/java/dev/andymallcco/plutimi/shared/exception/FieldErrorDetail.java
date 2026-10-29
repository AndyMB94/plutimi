package dev.andymallcco.plutimi.shared.exception;

/**
 * Un error de validación dentro de la propiedad {@code errors} de un ProblemDetail 400.
 *
 * @param field   campo del request o nombre del parámetro que falló
 * @param message mensaje en español para el usuario
 */
public record FieldErrorDetail(String field, String message) {
}
