package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void newCategoryIsActiveWithSlugDerivedFromName() {
        Category category = new Category("Alimento para Perros", "Croquetas y húmedos");

        assertThat(category.isActive()).isTrue();
        assertThat(category.getName()).isEqualTo("Alimento para Perros");
        assertThat(category.getSlug()).isEqualTo("alimento-para-perros");
        assertThat(category.getDescription()).isEqualTo("Croquetas y húmedos");
    }

    @Test
    void normalizesWhitespaceInName() {
        Category category = new Category("  Juguetes   para  gatos ", null);

        assertThat(category.getName()).isEqualTo("Juguetes para gatos");
        assertThat(category.getSlug()).isEqualTo("juguetes-para-gatos");
    }

    @Test
    void storesBlankDescriptionAsNull() {
        assertThat(new Category("Juguetes", "   ").getDescription()).isNull();
    }

    @Test
    void updateRegeneratesSlug() {
        Category category = new Category("Juguetes", null);

        category.update("Juguetes y Accesorios", "Pelotas, cuerdas y más");

        assertThat(category.getName()).isEqualTo("Juguetes y Accesorios");
        assertThat(category.getSlug()).isEqualTo("juguetes-y-accesorios");
        assertThat(category.getDescription()).isEqualTo("Pelotas, cuerdas y más");
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        Category category = new Category("Juguetes", null);

        category.deactivate();
        assertThat(category.isActive()).isFalse();

        category.activate();
        assertThat(category.isActive()).isTrue();
    }

}
