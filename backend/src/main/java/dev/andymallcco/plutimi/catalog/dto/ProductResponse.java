package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Detalle de un producto. Las especies se devuelven como IDs: pertenecen a otro módulo y el
 * frontend ya tiene la lista completa de especies (GET /api/v1/species).
 */
public record ProductResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "RC-AD-15")
        String sku,

        @Schema(example = "Royal Canin Adulto 15 kg")
        String name,

        @Schema(description = "Identificador legible para URLs, derivado de nombre y SKU",
                example = "royal-canin-adulto-15-kg-rc-ad-15")
        String slug,

        @Schema(example = "Alimento seco para perros adultos de raza mediana")
        String description,

        @Schema(example = "189.90")
        BigDecimal price,

        @Schema(description = "Código ISO 4217", example = "PEN")
        String currency,

        @Schema(example = "20")
        int stock,

        @Schema(example = "https://cdn.example.com/rc-adulto.jpg")
        String imageUrl,

        @Schema(example = "true")
        boolean active,

        CategoryRef category,

        @Schema(description = "Null si el producto no tiene marca")
        BrandRef brand,

        @Schema(example = "[1, 2]")
        Set<Long> speciesIds,

        Instant createdAt,

        Instant updatedAt) {

    public record CategoryRef(
            @Schema(example = "1") Long id,
            @Schema(example = "Alimento para perros") String name,
            @Schema(example = "alimento-para-perros") String slug) {
    }

    public record BrandRef(
            @Schema(example = "1") Long id,
            @Schema(example = "Royal Canin") String name,
            @Schema(example = "royal-canin") String slug) {
    }

}
