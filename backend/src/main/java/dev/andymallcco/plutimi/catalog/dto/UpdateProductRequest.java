package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Reemplazo completo de un producto (PUT). El stock no está aquí a propósito: se ajusta con
 * {@code PATCH /products/{id}/stock} para que un PUT no pise las ventas hechas entretanto.
 */
public record UpdateProductRequest(

        @Schema(description = "Código interno único. Se guarda en mayúsculas", example = "RC-AD-15")
        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 50, message = "El SKU no puede superar los 50 caracteres")
        @Pattern(regexp = CatalogValidation.SKU_FORMAT,
                message = "El SKU solo admite letras, números y guiones entre ellos")
        String sku,

        @Schema(example = "Royal Canin Adulto 15 kg")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        @Pattern(regexp = CatalogValidation.NAME_HAS_LETTER_OR_DIGIT,
                message = "El nombre debe contener al menos una letra o un número")
        String name,

        @Schema(description = "Descripción opcional", example = "Alimento seco para perros adultos de raza mediana")
        @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")
        String description,

        @Schema(description = "Categoría; si cambia, o si el producto queda activo, debe estar activa", example = "1")
        @NotNull(message = "La categoría es obligatoria")
        @Positive(message = "El id de la categoría debe ser positivo")
        Long categoryId,

        @Schema(description = "Marca; si cambia, debe estar activa. Null para quitarla", example = "1")
        @Positive(message = "El id de la marca debe ser positivo")
        Long brandId,

        @Schema(description = "Precio en soles, con hasta 2 decimales", example = "189.90")
        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 cifras enteras y 2 decimales")
        BigDecimal price,

        @Schema(description = "URL de la imagen (opcional)", example = "https://cdn.example.com/rc-adulto.jpg")
        @Size(max = 500, message = "La URL de la imagen no puede superar los 500 caracteres")
        @Pattern(regexp = CatalogValidation.HTTP_URL, message = "La imagen debe ser una URL http o https")
        String imageUrl,

        @Schema(description = "Especies a las que va dirigido (al menos una). Las nuevas deben estar activas", example = "[1, 2]")
        @NotEmpty(message = "Indica al menos una especie")
        Set<@NotNull(message = "El id de la especie es obligatorio")
            @Positive(message = "El id de la especie debe ser positivo") Long> speciesIds,

        @Schema(description = "Si el producto está a la venta", example = "true")
        @NotNull(message = "Indica si el producto está activo")
        Boolean active) {
}
