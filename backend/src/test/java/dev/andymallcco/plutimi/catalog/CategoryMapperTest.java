package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.andymallcco.plutimi.catalog.dto.CategoryResponse;
import org.junit.jupiter.api.Test;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void mapsAllFieldsToResponse() {
        Category category = new Category("Alimento para Perros", "Croquetas");
        category.deactivate();

        CategoryResponse response = mapper.toResponse(category);

        assertThat(response.name()).isEqualTo("Alimento para Perros");
        assertThat(response.slug()).isEqualTo("alimento-para-perros");
        assertThat(response.description()).isEqualTo("Croquetas");
        assertThat(response.active()).isFalse();
        // Sin persistir todavía: id y fechas de auditoría las asignan la base de datos y JPA
        assertThat(response.id()).isNull();
        assertThat(response.createdAt()).isNull();
    }

}
