package dev.andymallcco.plutimi.species.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reemplazo completo de una raza (PUT). La especie no se incluye porque es inmutable.
 * {@code active = true} reactiva una raza desactivada.
 */
public record UpdateBreedRequest(

        @Schema(description = "Nombre único dentro de la especie (sin distinguir mayúsculas)", example = "Labrador Retriever")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        @Pattern(regexp = SpeciesValidation.NAME_HAS_LETTER, message = "El nombre debe contener al menos una letra")
        String name,

        @Schema(description = "Si la raza está activa", example = "true")
        @NotNull(message = "Indica si la raza está activa")
        Boolean active) {
}
