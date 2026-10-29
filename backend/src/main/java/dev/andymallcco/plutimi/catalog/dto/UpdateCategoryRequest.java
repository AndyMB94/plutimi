package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reemplazo completo de una categoría (PUT): todos los campos obligatorios salvo la descripción.
 * {@code active = true} reactiva una categoría desactivada.
 */
public record UpdateCategoryRequest(

        @Schema(description = "Nombre único (sin distinguir mayúsculas)", example = "Alimento para perros")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Pattern(regexp = CategoryValidation.NAME_HAS_LETTER_OR_DIGIT,
                message = "El nombre debe contener al menos una letra o un número")
        String name,

        @Schema(description = "Descripción opcional", example = "Croquetas y alimento húmedo para perros de todas las edades")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,

        @Schema(description = "Si la categoría está activa (visible en el catálogo)", example = "true")
        @NotNull(message = "Indica si la categoría está activa")
        Boolean active) {
}
