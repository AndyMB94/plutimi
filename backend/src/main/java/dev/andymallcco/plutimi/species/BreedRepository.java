package dev.andymallcco.plutimi.species;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Package-private: los demás módulos acceden a las razas a través de SpeciesService.
 */
interface BreedRepository extends JpaRepository<Breed, Long> {

    // species.id se lee de la columna species_id: no hace falta un JOIN con species
    Page<Breed> findBySpeciesId(Long speciesId, Pageable pageable);

    Page<Breed> findBySpeciesIdAndActiveTrue(Long speciesId, Pageable pageable);

    // lower() para aprovechar el índice uk_breeds_species_id_name
    @Query("""
            select case when count(b) > 0 then true else false end
            from Breed b
            where b.species.id = :speciesId and lower(b.name) = lower(:name)
            """)
    boolean existsBySpeciesIdAndNameIgnoreCase(Long speciesId, String name);

    @Query("""
            select case when count(b) > 0 then true else false end
            from Breed b
            where b.species.id = :speciesId and lower(b.name) = lower(:name) and b.id <> :id
            """)
    boolean existsBySpeciesIdAndNameIgnoreCaseAndIdNot(Long speciesId, String name, Long id);

}
