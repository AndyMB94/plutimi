package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.species.dto.BreedResponse;
import dev.andymallcco.plutimi.species.dto.UpdateBreedRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operaciones sobre una raza concreta. Crear y listar razas se hace desde su especie
 * ({@code /api/v1/species/{id}/breeds}) en {@link SpeciesController}.
 */
@Tag(name = "Especies")
@RestController
@RequestMapping("/api/v1/breeds")
@RequiredArgsConstructor
public class BreedController {

    private final SpeciesService speciesService;

    @Operation(summary = "Obtener una raza", description = "Incluye las razas desactivadas.")
    @GetMapping("/{id}")
    public BreedResponse getById(@PathVariable @Positive Long id) {
        return speciesService.getBreed(id);
    }

    @Operation(summary = "Reemplazar una raza",
            description = "La especie no se puede cambiar. Con active=true reactiva una raza desactivada.")
    @PutMapping("/{id}")
    public BreedResponse update(@PathVariable @Positive Long id,
                                @Valid @RequestBody UpdateBreedRequest request) {
        return speciesService.updateBreed(id, request);
    }

    @Operation(summary = "Desactivar una raza", description = "Borrado lógico. Es idempotente.")
    @ApiResponse(responseCode = "204", description = "Raza desactivada")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable @Positive Long id) {
        speciesService.deactivateBreed(id);
    }

}
