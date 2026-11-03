package dev.andymallcco.plutimi.species;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Package-private: los demás módulos acceden a las especies a través de SpeciesService.
 */
interface SpeciesRepository extends JpaRepository<Species, Long> {

    Page<Species> findByActiveTrue(Pageable pageable);

    boolean existsByCode(String code);

    // lower() en lugar de IgnoreCase (que genera upper()) para aprovechar el índice uk_species_name
    @Query("""
            select case when count(s) > 0 then true else false end
            from Species s
            where lower(s.name) = lower(:name)
            """)
    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select case when count(s) > 0 then true else false end
            from Species s
            where lower(s.name) = lower(:name) and s.id <> :id
            """)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
