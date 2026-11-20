package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.catalog.dto.ProductFilter;
import dev.andymallcco.plutimi.shared.text.LikePatterns;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/**
 * Patrón Specification: cada método devuelve un criterio de búsqueda independiente y
 * {@link #from(ProductFilter)} combina con AND solo los que el cliente envió.
 * <p>
 * Cada Specification es una lambda {@code (root, query, cb) -> Predicate}: {@code root} es el
 * producto, {@code cb} (CriteriaBuilder) construye las condiciones y Hibernate genera el SQL.
 * Los nombres ("category", "price"...) son los atributos Java de {@link Product}, no las columnas.
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> from(ProductFilter filter) {
        Specification<Product> spec = Specification.unrestricted();
        if (!filter.includesInactive()) {
            spec = spec.and(isActive());
        }
        if (filter.categoryId() != null) {
            spec = spec.and(hasCategory(filter.categoryId()));
        }
        if (filter.brandId() != null) {
            spec = spec.and(hasBrand(filter.brandId()));
        }
        if (filter.speciesId() != null) {
            spec = spec.and(targetsSpecies(filter.speciesId()));
        }
        if (filter.searchText() != null) {
            spec = spec.and(nameOrSkuContains(filter.searchText()));
        }
        if (filter.minPrice() != null) {
            spec = spec.and(priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            spec = spec.and(priceAtMost(filter.maxPrice()));
        }
        if (filter.inStock() != null) {
            spec = spec.and(filter.inStock() ? inStock() : outOfStock());
        }
        return spec;
    }

    static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    static Specification<Product> hasCategory(Long categoryId) {
        // category.id se lee de la columna category_id: no hace falta JOIN con categories
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Product> hasBrand(Long brandId) {
        return (root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    /**
     * {@code isMember} genera una subconsulta {@code EXISTS} sobre product_species. Un JOIN
     * también funcionaría, pero multiplicaría filas si algún día se filtra por varias especies.
     */
    static Specification<Product> targetsSpecies(Long speciesId) {
        return (root, query, cb) -> cb.isMember(speciesId, root.<Set<Long>>get("speciesIds"));
    }

    /** Equivale a {@code name ILIKE '%texto%' OR sku ILIKE '%texto%'}. */
    static Specification<Product> nameOrSkuContains(String text) {
        String pattern = LikePatterns.containing(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, LikePatterns.ESCAPE),
                cb.like(cb.lower(root.get("sku")), pattern, LikePatterns.ESCAPE));
    }

    static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price").get("amount"), min);
    }

    static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price").get("amount"), max);
    }

    static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
    }

    static Specification<Product> outOfStock() {
        return (root, query, cb) -> cb.equal(root.get("stock"), 0);
    }

}
