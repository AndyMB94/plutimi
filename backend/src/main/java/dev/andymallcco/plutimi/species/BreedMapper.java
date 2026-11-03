package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.species.dto.BreedResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad en DTO de respuesta. Solo expone el id de la especie: leerlo del proxy
 * LAZY no lanza una consulta, así que listar razas no carga sus especies.
 */
@Component
class BreedMapper {

    BreedResponse toResponse(Breed breed) {
        return new BreedResponse(
                breed.getId(),
                breed.getSpecies().getId(),
                breed.getName(),
                breed.isActive(),
                breed.getCreatedAt(),
                breed.getUpdatedAt());
    }

}
