package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrandTest {

    @Test
    void newBrandIsActiveWithNormalizedNameAndSlug() {
        Brand brand = new Brand("  Royal   Canin ");

        assertThat(brand.isActive()).isTrue();
        assertThat(brand.getName()).isEqualTo("Royal Canin");
        assertThat(brand.getSlug()).isEqualTo("royal-canin");
    }

    @Test
    void renameRegeneratesSlug() {
        Brand brand = new Brand("Pro Plan");

        brand.rename("Purina Pro Plan");

        assertThat(brand.getName()).isEqualTo("Purina Pro Plan");
        assertThat(brand.getSlug()).isEqualTo("purina-pro-plan");
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        Brand brand = new Brand("Royal Canin");

        brand.deactivate();
        assertThat(brand.isActive()).isFalse();

        brand.activate();
        assertThat(brand.isActive()).isTrue();
    }

}
