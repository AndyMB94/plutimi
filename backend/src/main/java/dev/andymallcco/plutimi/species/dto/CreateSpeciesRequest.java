package dev.andymallcco.plutimi.species.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSpeciesRequest(

        @Schema(description = "Código único e inmutable, en mayúsculas", example = "GUINEA_PIG")
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        @Pattern(regexp = SpeciesValidation.CODE_FORMAT,
                message = "El código solo puede tener letras mayúsculas sin tilde y guiones bajos, y debe empezar por una letra")
        String code,

        @Schema(description = "Nombre visible único (sin distinguir mayúsculas)", example = "Cuy")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
        @Pattern(regexp = SpeciesValidation.NAME_HAS_LETTER, message = "El nombre debe contener al menos una letra")
        String name) {
}
