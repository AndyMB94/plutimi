package dev.andymallcco.plutimi.catalog.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Filtros opcionales del catálogo, recibidos como parámetros de consulta
 * ({@code ?speciesId=2&maxPrice=50&inStock=true}). Spring crea el record con el constructor a
 * partir de los parámetros que lleguen; los que falten quedan en null y no filtran.
 */
public record ProductFilter(

        @Parameter(description = "Productos de esta categoría")
        @Positive(message = "El id de la categoría debe ser positivo")
        Long categoryId,

        @Parameter(description = "Productos de esta marca")
        @Positive(message = "El id de la marca debe ser positivo")
        Long brandId,

        @Parameter(description = "Productos dirigidos a esta especie")
        @Positive(message = "El id de la especie debe ser positivo")
        Long speciesId,

        @Parameter(description = "Texto contenido en el nombre o el SKU (sin distinguir mayúsculas)")
        @Size(max = 100, message = "El texto de búsqueda no puede superar los 100 caracteres")
        String q,

        @Parameter(description = "Precio mínimo en soles (incluido)")
        @PositiveOrZero(message = "El precio mínimo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio mínimo admite hasta 2 decimales")
        BigDecimal minPrice,

        @Parameter(description = "Precio máximo en soles (incluido)")
        @PositiveOrZero(message = "El precio máximo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio máximo admite hasta 2 decimales")
        BigDecimal maxPrice,

        @Parameter(description = "true: solo con stock; false: solo agotados; sin valor: todos")
        Boolean inStock,

        @Parameter(description = "Incluir también los productos desactivados")
        Boolean includeInactive) {

    public static ProductFilter none() {
        return new ProductFilter(null, null, null, null, null, null, null, null);
    }

    /** Texto de búsqueda sin espacios sobrantes, o null si no hay nada que buscar. */
    public String searchText() {
        return q == null || q.isBlank() ? null : q.strip();
    }

    public boolean includesInactive() {
        return Boolean.TRUE.equals(includeInactive);
    }

    @Parameter(hidden = true)
    @AssertTrue(message = "El precio mínimo no puede ser mayor que el máximo")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

}
