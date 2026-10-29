package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.andymallcco.plutimi.catalog.dto.CategoryResponse;
import dev.andymallcco.plutimi.catalog.dto.CreateCategoryRequest;
import dev.andymallcco.plutimi.catalog.dto.UpdateCategoryRequest;
import dev.andymallcco.plutimi.shared.web.PageResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Test unitario: el repositorio es un mock de Mockito, sin Spring ni base de datos.
 * Las consultas reales se prueban en el test de integración.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long ID = 7L;

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, new CategoryMapper());
    }

    // --- create ---

    @Test
    void createSavesCategoryWithNormalizedNameAndSlug() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.create(new CreateCategoryRequest("  Alimento   para Perros ", null));

        assertThat(response.name()).isEqualTo("Alimento para Perros");
        assertThat(response.slug()).isEqualTo("alimento-para-perros");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(categoryRepository.existsByNameIgnoreCase("juguetes")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("juguetes", null)))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessageContaining("nombre 'juguetes'");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createRejectsNameWithEquivalentSlug() {
        when(categoryRepository.existsBySlug("perros-gatos")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("Perros & Gatos", null)))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessageContaining("perros-gatos");
        verify(categoryRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void updateChangesNameDescriptionAndActiveFlag() {
        Category category = new Category("Juguetes", null);
        when(categoryRepository.findById(ID)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.update(ID,
                new UpdateCategoryRequest("Juguetes y Accesorios", "Pelotas y cuerdas", false));

        assertThat(response.slug()).isEqualTo("juguetes-y-accesorios");
        assertThat(response.description()).isEqualTo("Pelotas y cuerdas");
        assertThat(response.active()).isFalse();
        verify(categoryRepository).flush();
    }

    @Test
    void updateCanReactivateCategory() {
        Category category = new Category("Juguetes", null);
        category.deactivate();
        when(categoryRepository.findById(ID)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.update(ID, new UpdateCategoryRequest("Juguetes", null, true));

        assertThat(response.active()).isTrue();
    }

    @Test
    void updateRejectsNameUsedByAnotherCategory() {
        Category category = new Category("Juguetes", null);
        when(categoryRepository.findById(ID)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Accesorios", ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(ID, new UpdateCategoryRequest("Accesorios", null, true)))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        // La entidad no se modificó: la validación ocurre antes del update
        assertThat(category.getName()).isEqualTo("Juguetes");
    }

    @Test
    void updateThrowsNotFoundForUnknownId() {
        when(categoryRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(ID, new UpdateCategoryRequest("Juguetes", null, true)))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("No existe una categoría con id 7");
    }

    // --- getById / list ---

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        when(categoryRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(ID)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void listReturnsOnlyActiveCategoriesByDefault() {
        Pageable pageable = PageRequest.of(0, 20);
        when(categoryRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(new Category("Juguetes", null)), pageable, 1));

        PageResponse<CategoryResponse> page = categoryService.list(false, pageable);

        assertThat(page.content()).extracting(CategoryResponse::name).containsExactly("Juguetes");
        assertThat(page.totalElements()).isEqualTo(1);
        verify(categoryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listIncludesInactiveCategoriesWhenRequested() {
        Pageable pageable = PageRequest.of(0, 20);
        when(categoryRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        categoryService.list(true, pageable);

        verify(categoryRepository, never()).findByActiveTrue(any());
    }

    // --- deactivate ---

    @Test
    void deactivateMarksCategoryAsInactive() {
        Category category = new Category("Juguetes", null);
        when(categoryRepository.findById(ID)).thenReturn(Optional.of(category));

        categoryService.deactivate(ID);

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void deactivateThrowsNotFoundForUnknownId() {
        when(categoryRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deactivate(ID)).isInstanceOf(CategoryNotFoundException.class);
    }

}
