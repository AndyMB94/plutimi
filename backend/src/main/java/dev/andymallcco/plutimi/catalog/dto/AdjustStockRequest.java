package dev.andymallcco.plutimi.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Ajuste manual de stock con una variación, no con el valor final: así no se pisan las unidades
 * vendidas mientras el administrador hacía el recuento.
 */
public record AdjustStockRequest(

        @Schema(description = "Unidades a sumar (positivo) o restar (negativo). No puede ser 0", example = "-3")
        @NotNull(message = "La variación de stock es obligatoria")
        @Min(value = -CatalogValidation.MAX_STOCK_DELTA, message = "La variación no puede ser menor que -1000000")
        @Max(value = CatalogValidation.MAX_STOCK_DELTA, message = "La variación no puede ser mayor que 1000000")
        Integer delta) {

    /**
     * Regla que ninguna anotación estándar expresa ("distinto de cero"). Bean Validation trata el
     * método {@code isDeltaNonZero} como la propiedad {@code deltaNonZero}. No forma parte del JSON.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "La variación de stock no puede ser 0")
    public boolean isDeltaNonZero() {
        return delta == null || delta != 0;
    }

}
