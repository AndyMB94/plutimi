package dev.andymallcco.plutimi.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Package-private: los demás módulos acceden a las marcas a través de BrandService.
 */
interface BrandRepository extends JpaRepository<Brand, Long> {

    Page<Brand> findByActiveTrue(Pageable pageable);

    // lower() en lugar de IgnoreCase (que genera upper()) para aprovechar el índice uk_brands_name
    @Query("""
            select case when count(b) > 0 then true else false end
            from Brand b
            where lower(b.name) = lower(:name)
            """)
    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select case when count(b) > 0 then true else false end
            from Brand b
            where lower(b.name) = lower(:name) and b.id <> :id
            """)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

}
