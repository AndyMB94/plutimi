package dev.andymallcco.plutimi.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Package-private: los demás módulos acceden a los productos a través de ProductService.
 * <p>
 * {@link JpaSpecificationExecutor} añade {@code findAll(Specification, Pageable)}: el catálogo
 * combina filtros opcionales en tiempo de ejecución (ver ProductSpecifications).
 */
interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // El SKU se guarda normalizado en mayúsculas: basta la comparación exacta
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** ¿Hay productos a la venta en la categoría? Usa el índice ix_products_category_id. */
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

}
