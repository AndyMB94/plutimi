package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.BrandResponse;
import org.springframework.stereotype.Component;

@Component
class BrandMapper {

    BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.isActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt());
    }

}
