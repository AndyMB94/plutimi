package dev.andymallcco.plutimi.species;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SpeciesTest {

    @Test
    void newSpeciesIsActive() {
        Species species = new Species("DOG", "Perro");

        assertThat(species.isActive()).isTrue();
        assertThat(species.getCode()).isEqualTo("DOG");
        assertThat(species.getName()).isEqualTo("Perro");
    }

    @Test
    void normalizesWhitespaceInName() {
        assertThat(new Species("GUINEA_PIG", "  Cuy   doméstico ").getName()).isEqualTo("Cuy doméstico");
    }

    @Test
    void updateChangesNameButKeepsCode() {
        Species species = new Species("DOG", "Perro");

        species.update("Perros");

        assertThat(species.getName()).isEqualTo("Perros");
        assertThat(species.getCode()).isEqualTo("DOG");
    }

    @Test
    void newBreedBelongsToThisSpecies() {
        Species species = new Species("DOG", "Perro");

        Breed breed = species.newBreed("Beagle");

        assertThat(breed.getSpecies()).isSameAs(species);
        assertThat(breed.getName()).isEqualTo("Beagle");
    }

    @Test
    void inactiveSpeciesCannotHaveNewBreeds() {
        Species species = new Species("DOG", "Perro");
        species.deactivate();

        assertThatThrownBy(() -> species.newBreed("Beagle"))
                .isInstanceOf(InactiveSpeciesException.class)
                .hasMessage("La especie 'Perro' está desactivada");
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        Species species = new Species("DOG", "Perro");

        species.deactivate();
        assertThat(species.isActive()).isFalse();

        species.activate();
        assertThat(species.isActive()).isTrue();
    }

}
