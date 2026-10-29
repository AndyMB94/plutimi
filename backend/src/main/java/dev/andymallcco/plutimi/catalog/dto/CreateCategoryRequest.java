package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @Schema(description = "Nombre único (sin distinguir mayúsculas)", example = "Alimento para perros")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Pattern(regexp = CategoryValidation.NAME_HAS_LETTER_OR_DIGIT,
                message = "El nombre debe contener al menos una letra o un número")
        String name,

        @Schema(description = "Descripción opcional", example = "Croquetas y alimento húmedo para perros de todas las edades")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description) {
}
