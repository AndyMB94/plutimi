package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.ProductResponse;
import dev.andymallcco.plutimi.catalog.dto.ProductSummaryResponse;
import dev.andymallcco.plutimi.catalog.dto.ProductResponse.BrandRef;
import dev.andymallcco.plutimi.catalog.dto.ProductResponse.CategoryRef;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Se llama dentro de la transacción del service: category, brand y speciesIds son LAZY y se
 * cargan aquí al acceder a ellos.
 */
@Component
class ProductMapper {

    ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        Brand brand = product.getBrand();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice().amount(),
                product.getPrice().currency().getCurrencyCode(),
                product.getStock(),
                product.getImageUrl(),
                product.isActive(),
                new CategoryRef(category.getId(), category.getName(), category.getSlug()),
                brand == null ? null : new BrandRef(brand.getId(), brand.getName(), brand.getSlug()),
                // Copia: el DTO no debe quedar enlazado a la colección gestionada por Hibernate
                Set.copyOf(product.getSpeciesIds()),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    /**
     * La marca llega ya cargada (@EntityGraph en ProductRepository). De la categoría solo se usa
     * el id, que el proxy LAZY conoce sin consultar la base de datos.
     */
    ProductSummaryResponse toSummaryResponse(Product product) {
        Brand brand = product.getBrand();
        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getPrice().amount(),
                product.getPrice().currency().getCurrencyCode(),
                product.getStock(),
                product.getImageUrl(),
                product.isActive(),
                product.getCategory().getId(),
                brand == null ? null : new BrandRef(brand.getId(), brand.getName(), brand.getSlug()));
    }

}
