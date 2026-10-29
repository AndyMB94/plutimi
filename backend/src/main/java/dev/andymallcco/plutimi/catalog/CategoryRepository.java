package dev.andymallcco.plutimi.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Package-private: los demás módulos acceden a las categorías a través de CategoryService.
 */
interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findByActiveTrue(Pageable pageable);

    // lower() en lugar de IgnoreCase (que genera upper()) para aprovechar el índice uk_categories_name
    @Query("""
            select case when count(c) > 0 then true else false end
            from Category c
            where lower(c.name) = lower(:name)
            """)
    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select case when count(c) > 0 then true else false end
            from Category c
            where lower(c.name) = lower(:name) and c.id <> :id
            """)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

}
