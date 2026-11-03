package dev.andymallcco.plutimi.species.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** La especie no va en el cuerpo: es la de la ruta {@code /species/{id}/breeds}. */
public record CreateBreedRequest(

        @Schema(description = "Nombre único dentro de la especie (sin distinguir mayúsculas)", example = "Labrador Retriever")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        @Pattern(regexp = SpeciesValidation.NAME_HAS_LETTER, message = "El nombre debe contener al menos una letra")
        String name) {
}
