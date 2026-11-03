package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad en DTO de respuesta. No hay conversión inversa: las entidades se crean
 * y modifican con sus métodos de negocio en el service.
 */
@Component
class SpeciesMapper {

    SpeciesResponse toResponse(Species species) {
        return new SpeciesResponse(
                species.getId(),
                species.getCode(),
                species.getName(),
                species.isActive(),
                species.getCreatedAt(),
                species.getUpdatedAt());
    }

}
