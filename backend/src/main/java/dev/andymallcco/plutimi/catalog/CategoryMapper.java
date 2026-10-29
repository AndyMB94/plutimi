package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.CategoryResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad en DTO de respuesta. No hay conversión inversa: las entidades se crean
 * y modifican con sus métodos de negocio en el service.
 */
@Component
class CategoryMapper {

    CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

}
