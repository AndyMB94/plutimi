package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record BrandResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "Royal Canin")
        String name,

        @Schema(description = "Identificador legible para URLs, derivado del nombre", example = "royal-canin")
        String slug,

        @Schema(example = "true")
        boolean active,

        Instant createdAt,

        Instant updatedAt) {
}
