package dev.andymallcco.plutimi.species;

import static org.assertj.core.api.Assertions.assertThat;

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
    void canBeDeactivatedAndReactivated() {
        Species species = new Species("DOG", "Perro");

        species.deactivate();
        assertThat(species.isActive()).isFalse();

        species.activate();
        assertThat(species.isActive()).isTrue();
    }

}
