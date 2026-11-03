package dev.andymallcco.plutimi.species.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SpeciesResponse(

        @Schema(example = "1")
        Long id,

        @Schema(description = "Código estable de la especie", example = "DOG")
        String code,

        @Schema(example = "Perro")
        String name,

        @Schema(example = "true")
        boolean active,

        Instant createdAt,

        Instant updatedAt) {
}
