package dev.andymallcco.plutimi.species;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BreedTest {

    private final Species dog = new Species("DOG", "Perro");

    @Test
    void newBreedIsActiveAndBelongsToItsSpecies() {
        Breed breed = new Breed(dog, "Labrador Retriever");

        assertThat(breed.isActive()).isTrue();
        assertThat(breed.getSpecies()).isSameAs(dog);
        assertThat(breed.getName()).isEqualTo("Labrador Retriever");
    }

    @Test
    void normalizesWhitespaceInName() {
        assertThat(new Breed(dog, " Pastor   Alemán  ").getName()).isEqualTo("Pastor Alemán");
    }

    @Test
    void updateChangesNameButKeepsSpecies() {
        Breed breed = new Breed(dog, "Labrador");

        breed.update("Labrador Retriever");

        assertThat(breed.getName()).isEqualTo("Labrador Retriever");
        assertThat(breed.getSpecies()).isSameAs(dog);
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        Breed breed = new Breed(dog, "Labrador Retriever");

        breed.deactivate();
        assertThat(breed.isActive()).isFalse();

        breed.activate();
        assertThat(breed.isActive()).isTrue();
    }

}
