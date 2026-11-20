package dev.andymallcco.plutimi.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Package-private: los demás módulos acceden a los productos a través de ProductService.
 * <p>
 * {@link JpaSpecificationExecutor} añade {@code findAll(Specification, Pageable)}: el catálogo
 * combina filtros opcionales en tiempo de ejecución (ver ProductSpecifications).
 */
interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Listado del catálogo. {@code @EntityGraph} trae la marca en la misma consulta (LEFT JOIN):
     * sin él, cada producto de la página dispararía su propio SELECT de brands (problema N+1),
     * como olvidar {@code select_related("brand")} en Django.
     */
    @Override
    @EntityGraph(attributePaths = "brand")
    Page<Product> findAll(@Nullable Specification<Product> spec, Pageable pageable);

    // El SKU se guarda normalizado en mayúsculas: basta la comparación exacta
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** ¿Hay productos a la venta en la categoría? Usa el índice ix_products_category_id. */
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

}
