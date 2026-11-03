package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.species.dto.BreedResponse;
import dev.andymallcco.plutimi.species.dto.CreateBreedRequest;
import dev.andymallcco.plutimi.species.dto.CreateSpeciesRequest;
import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import dev.andymallcco.plutimi.species.dto.UpdateSpeciesRequest;
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
import org.springframework.data.web.PageableDefault;
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

/**
 * Especies y la creación y listado de sus razas (rutas anidadas). Las operaciones sobre una raza
 * concreta están en {@link BreedController}.
 * <p>
 * Los listados usan páginas de 100 por defecto: son datos de referencia pequeños que el frontend
 * carga completos para los desplegables.
 */
@Tag(name = "Especies", description = "Especies y razas (datos de referencia)")
@RestController
@RequestMapping("/api/v1/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @Operation(summary = "Listar especies", description = "Paginado y ordenado por nombre. Por defecto solo las activas.")
    @GetMapping
    public PageResponse<SpeciesResponse> list(
            @Parameter(description = "Incluir también las especies desactivadas")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ParameterObject @PageableDefault(size = 100, sort = "name") Pageable pageable) {
        return speciesService.listSpecies(includeInactive, pageable);
    }

    @Operation(summary = "Obtener una especie", description = "Incluye las especies desactivadas.")
    @GetMapping("/{id}")
    public SpeciesResponse getById(@PathVariable @Positive Long id) {
        return speciesService.getSpecies(id);
    }

    @Operation(summary = "Crear una especie")
    @ApiResponse(responseCode = "201", description = "Especie creada",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la especie creada",
                    schema = @Schema(type = "string", format = "uri")))
    @PostMapping
    public ResponseEntity<SpeciesResponse> create(@Valid @RequestBody CreateSpeciesRequest request) {
        SpeciesResponse created = speciesService.createSpecies(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Reemplazar una especie",
            description = "El código no se puede cambiar. Con active=true reactiva una especie desactivada.")
    @PutMapping("/{id}")
    public SpeciesResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody UpdateSpeciesRequest request) {
        return speciesService.updateSpecies(id, request);
    }

    @Operation(summary = "Desactivar una especie",
            description = "Borrado lógico. Es idempotente. Sus razas no se desactivan.")
    @ApiResponse(responseCode = "204", description = "Especie desactivada")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Positive Long id) {
        speciesService.deactivateSpecies(id);
    }

    @Operation(summary = "Listar las razas de una especie",
            description = "Paginado y ordenado por nombre. Por defecto solo las activas.")
    @GetMapping("/{id}/breeds")
    public PageResponse<BreedResponse> listBreeds(
            @PathVariable @Positive Long id,
            @Parameter(description = "Incluir también las razas desactivadas")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ParameterObject @PageableDefault(size = 100, sort = "name") Pageable pageable) {
        return speciesService.listBreeds(id, includeInactive, pageable);
    }

    @Operation(summary = "Crear una raza en una especie", description = "La especie debe estar activa.")
    @ApiResponse(responseCode = "201", description = "Raza creada",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la raza creada (/api/v1/breeds/{id})",
                    schema = @Schema(type = "string", format = "uri")))
    @PostMapping("/{id}/breeds")
    public ResponseEntity<BreedResponse> createBreed(@PathVariable @Positive Long id,
                                                     @Valid @RequestBody CreateBreedRequest request) {
        BreedResponse created = speciesService.createBreed(id, request);
        // La raza creada vive en /api/v1/breeds/{id}, no bajo la ruta de la especie
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/breeds/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

}
