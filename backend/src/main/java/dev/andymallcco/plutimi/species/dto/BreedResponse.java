package dev.andymallcco.plutimi.species.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record BreedResponse(

        @Schema(example = "4")
        Long id,

        @Schema(description = "Especie a la que pertenece la raza", example = "1")
        Long speciesId,

        @Schema(example = "Labrador Retriever")
        String name,

        @Schema(example = "true")
        boolean active,

        Instant createdAt,

        Instant updatedAt) {
}
