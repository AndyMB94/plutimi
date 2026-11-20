package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.AdjustStockRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateProductRequest;
import dev.andymallcco.plutimi.catalog.dto.ProductFilter;
import dev.andymallcco.plutimi.catalog.dto.ProductResponse;
import dev.andymallcco.plutimi.catalog.dto.ProductSummaryResponse;
import dev.andymallcco.plutimi.catalog.dto.UpdateProductRequest;
import dev.andymallcco.plutimi.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Catálogo de productos. Los GET son públicos; la escritura se protegerá con
 * {@code catalog:manage} en la sección 1.7.
 */
@Tag(name = "Productos", description = "Catálogo de productos con filtros")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Buscar productos",
            description = "Paginado. Todos los filtros son opcionales y se combinan con AND. Por defecto solo los "
                    + "activos, ordenados por nombre. Otros órdenes: sort=price.amount,asc o sort=createdAt,desc.")
    @GetMapping
    public PageResponse<ProductSummaryResponse> list(
            // Sin anotación de Spring, un record se rellena con los parámetros de la URL (como @ModelAttribute).
            // @ParameterObject hace que Swagger muestre cada campo como un parámetro de consulta.
            @ParameterObject @Valid ProductFilter filter,
            @ParameterObject @SortDefault(sort = "name") Pageable pageable) {
        return productService.list(filter, pageable);
    }

    @Operation(summary = "Obtener un producto", description = "Con marca, categoría y especies. Incluye los desactivados.")
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable @Positive Long id) {
        return productService.getById(id);
    }

    @Operation(summary = "Crear un producto",
            description = "La categoría, la marca (opcional) y las especies deben existir y estar activas.")
    @ApiResponse(responseCode = "201", description = "Producto creado",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL del producto creado",
                    schema = @Schema(type = "string", format = "uri")))
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Reemplazar un producto",
            description = "Todo salvo el stock, que se ajusta con PATCH /{id}/stock. Con active=true reactiva el producto.")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Ajustar el stock",
            description = "Suma (delta positivo) o resta (negativo) unidades. 422 si el stock quedaría negativo; "
                    + "409 si otro ajuste modificó el producto a la vez.")
    @PatchMapping("/{id}/stock")
    public ProductResponse adjustStock(@PathVariable @Positive Long id,
                                       @Valid @RequestBody AdjustStockRequest request) {
        return productService.adjustStock(id, request);
    }

    @Operation(summary = "Desactivar un producto", description = "Borrado lógico. Es idempotente.")
    @ApiResponse(responseCode = "204", description = "Producto desactivado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Positive Long id) {
        productService.deactivate(id);
    }

}
