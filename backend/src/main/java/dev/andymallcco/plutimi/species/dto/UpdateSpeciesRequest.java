package dev.andymallcco.plutimi.species.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reemplazo completo de una especie (PUT). El código no se incluye porque es inmutable.
 * {@code active = true} reactiva una especie desactivada.
 */
public record UpdateSpeciesRequest(

        @Schema(description = "Nombre visible único (sin distinguir mayúsculas)", example = "Perro")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
        @Pattern(regexp = SpeciesValidation.NAME_HAS_LETTER, message = "El nombre debe contener al menos una letra")
        String name,

        @Schema(description = "Si la especie está activa (disponible en el catálogo y la veterinaria)", example = "true")
        @NotNull(message = "Indica si la especie está activa")
        Boolean active) {
}
