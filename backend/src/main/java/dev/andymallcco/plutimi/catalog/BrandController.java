package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.BrandResponse;
import dev.andymallcco.plutimi.catalog.dto.CreateBrandRequest;
import dev.andymallcco.plutimi.catalog.dto.UpdateBrandRequest;
import dev.andymallcco.plutimi.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Marcas", description = "Marcas de los productos del catálogo")
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @Operation(summary = "Listar marcas", description = "Paginado y ordenado por nombre. Por defecto solo las activas.")
    @GetMapping
    public PageResponse<BrandResponse> list(
            @Parameter(description = "Incluir también las marcas desactivadas")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ParameterObject @SortDefault(sort = "name") Pageable pageable) {
        return brandService.list(includeInactive, pageable);
    }

    @Operation(summary = "Obtener una marca", description = "Incluye las marcas desactivadas.")
    @GetMapping("/{id}")
    public BrandResponse getById(@PathVariable @Positive Long id) {
        return brandService.getById(id);
    }

    @Operation(summary = "Crear una marca")
    @ApiResponse(responseCode = "201", description = "Marca creada",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la marca creada",
                    schema = @Schema(type = "string", format = "uri")))
    @PostMapping
    public ResponseEntity<BrandResponse> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandResponse created = brandService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Reemplazar una marca", description = "Con active=true reactiva una marca desactivada.")
    @PutMapping("/{id}")
    public BrandResponse update(@PathVariable @Positive Long id,
                                @Valid @RequestBody UpdateBrandRequest request) {
        return brandService.update(id, request);
    }

    @Operation(summary = "Desactivar una marca",
            description = "Borrado lógico. Es idempotente. Sus productos siguen a la venta.")
    @ApiResponse(responseCode = "204", description = "Marca desactivada")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Positive Long id) {
        brandService.deactivate(id);
    }

}
