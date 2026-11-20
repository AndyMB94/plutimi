package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(

        @Schema(description = "Nombre único (sin distinguir mayúsculas)", example = "Royal Canin")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Pattern(regexp = CatalogValidation.NAME_HAS_LETTER_OR_DIGIT,
                message = "El nombre debe contener al menos una letra o un número")
        String name) {
}
