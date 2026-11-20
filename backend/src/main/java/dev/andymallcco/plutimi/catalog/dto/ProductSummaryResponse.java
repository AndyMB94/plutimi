package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Producto en el listado del catálogo: lo necesario para una tarjeta. Sin descripción ni
 * especies, para que listar una página no cargue colecciones de cada producto.
 */
public record ProductSummaryResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "RC-AD-15")
        String sku,

        @Schema(example = "Royal Canin Adulto 15 kg")
        String name,

        @Schema(example = "royal-canin-adulto-15-kg-rc-ad-15")
        String slug,

        @Schema(example = "189.90")
        BigDecimal price,

        @Schema(example = "PEN")
        String currency,

        @Schema(example = "20")
        int stock,

        @Schema(example = "https://cdn.example.com/rc-adulto.jpg")
        String imageUrl,

        @Schema(example = "true")
        boolean active,

        @Schema(example = "1")
        Long categoryId,

        @Schema(description = "Null si el producto no tiene marca")
        ProductResponse.BrandRef brand) {
}
