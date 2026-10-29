package dev.andymallcco.plutimi.shared.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void lowercasesAndJoinsWordsWithDashes() {
        assertThat(Slugs.of("Alimento para Perros")).isEqualTo("alimento-para-perros");
    }

    @Test
    void removesAccentsAndEnye() {
        assertThat(Slugs.of("Ñandú Pequeño Camión")).isEqualTo("nandu-pequeno-camion");
    }

    @Test
    void collapsesSymbolsAndRepeatedSpaces() {
        assertThat(Slugs.of("Perros  &  Gatos!!")).isEqualTo("perros-gatos");
    }

    @Test
    void trimsLeadingAndTrailingSeparators() {
        assertThat(Slugs.of("  -- Juguetes --  ")).isEqualTo("juguetes");
    }

    @Test
    void keepsDigits() {
        assertThat(Slugs.of("Arena 10 kg")).isEqualTo("arena-10-kg");
    }

    @Test
    void rejectsTextWithoutLettersOrDigits() {
        assertThatThrownBy(() -> Slugs.of("¡¡!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Slugs.of(null))
                .isInstanceOf(NullPointerException.class);
    }

}
