package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CategoryResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "Alimento para perros")
        String name,

        @Schema(description = "Identificador legible para URLs, derivado del nombre", example = "alimento-para-perros")
        String slug,

        @Schema(example = "Croquetas y alimento húmedo para perros de todas las edades")
        String description,

        @Schema(example = "true")
        boolean active,

        Instant createdAt,

        Instant updatedAt) {
}
