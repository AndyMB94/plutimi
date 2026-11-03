package dev.andymallcco.plutimi.species;

import static org.assertj.core.api.Assertions.assertThat;

import dev.andymallcco.plutimi.species.dto.BreedResponse;
import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Cubre los dos mappers del módulo: son triviales y comparten los datos de prueba. */
class SpeciesMapperTest {

    private final SpeciesMapper speciesMapper = new SpeciesMapper();
    private final BreedMapper breedMapper = new BreedMapper();

    @Test
    void mapsAllSpeciesFieldsToResponse() {
        Species species = new Species("DOG", "Perro");
        species.deactivate();

        SpeciesResponse response = speciesMapper.toResponse(species);

        assertThat(response.code()).isEqualTo("DOG");
        assertThat(response.name()).isEqualTo("Perro");
        assertThat(response.active()).isFalse();
        // Sin persistir todavía: id y fechas de auditoría las asignan la base de datos y JPA
        assertThat(response.id()).isNull();
        assertThat(response.createdAt()).isNull();
    }

    @Test
    void mapsBreedWithItsSpeciesId() {
        Species species = new Species("DOG", "Perro");
        // Simula una especie ya persistida: el id lo asigna la base de datos
        ReflectionTestUtils.setField(species, "id", 1L);
        Breed breed = new Breed(species, "Labrador Retriever");

        BreedResponse response = breedMapper.toResponse(breed);

        assertThat(response.speciesId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Labrador Retriever");
        assertThat(response.active()).isTrue();
        assertThat(response.id()).isNull();
    }

}
