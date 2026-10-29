package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.CategoryResponse;
import dev.andymallcco.plutimi.catalog.dto.CreateCategoryRequest;
import dev.andymallcco.plutimi.catalog.dto.UpdateCategoryRequest;
import dev.andymallcco.plutimi.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
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

@Tag(name = "Categorías", description = "Categorías del catálogo de productos")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Listar categorías", description = "Paginado y ordenado por nombre. Por defecto solo las activas.")
    @GetMapping
    public PageResponse<CategoryResponse> list(
            @Parameter(description = "Incluir también las categorías desactivadas")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ParameterObject @SortDefault(sort = "name") Pageable pageable) {
        return categoryService.list(includeInactive, pageable);
    }

    @Operation(summary = "Obtener una categoría", description = "Incluye las categorías desactivadas.")
    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable @Positive Long id) {
        return categoryService.getById(id);
    }

    @Operation(summary = "Crear una categoría")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Reemplazar una categoría", description = "Con active=true reactiva una categoría desactivada.")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable @Positive Long id,
                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @Operation(summary = "Desactivar una categoría", description = "Borrado lógico. Es idempotente.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Positive Long id) {
        categoryService.deactivate(id);
    }

}
