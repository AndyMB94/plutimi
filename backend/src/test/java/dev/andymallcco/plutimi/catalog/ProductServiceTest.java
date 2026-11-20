package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.andymallcco.plutimi.catalog.dto.AdjustStockRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateProductRequest;
import dev.andymallcco.plutimi.catalog.dto.ProductResponse;
import dev.andymallcco.plutimi.catalog.dto.UpdateProductRequest;
import dev.andymallcco.plutimi.shared.money.Money;
import dev.andymallcco.plutimi.species.InactiveSpeciesException;
import dev.andymallcco.plutimi.species.SpeciesService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test unitario: repositorios y SpeciesService son mocks. Las consultas y la persistencia
 * reales se prueban en el test de integración.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long PRODUCT_ID = 50L;
    private static final Long FOOD_ID = 1L;
    private static final Long TOYS_ID = 2L;
    private static final Long ROYAL_CANIN_ID = 10L;
    private static final Long PRO_PLAN_ID = 11L;
    private static final Long DOG = 1L;
    private static final Long CAT = 2L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SpeciesService speciesService;

    private ProductService productService;

    private Category food;
    private Brand royalCanin;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, brandRepository,
                speciesService, new ProductMapper());
        food = withId(new Category("Alimento", null), FOOD_ID);
        royalCanin = withId(new Brand("Royal Canin"), ROYAL_CANIN_ID);
    }

    @Nested
    class Create {

        @Test
        void savesProductWithNormalizedSkuAndValidatedReferences() {
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));
            when(brandRepository.findById(ROYAL_CANIN_ID)).thenReturn(Optional.of(royalCanin));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.create(createRequest(" rc-ad-15 ", ROYAL_CANIN_ID, Set.of(DOG, CAT)));

            assertThat(response.sku()).isEqualTo("RC-AD-15");
            assertThat(response.slug()).isEqualTo("adulto-15-kg-rc-ad-15");
            assertThat(response.price()).isEqualByComparingTo("189.90");
            assertThat(response.currency()).isEqualTo("PEN");
            assertThat(response.category().name()).isEqualTo("Alimento");
            assertThat(response.brand().name()).isEqualTo("Royal Canin");
            assertThat(response.speciesIds()).containsExactlyInAnyOrder(DOG, CAT);
            verify(speciesService).assertActiveSpecies(Set.of(DOG, CAT));
        }

        @Test
        void brandIsOptional() {
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductResponse response = productService.create(createRequest("GEN-1", null, Set.of(DOG)));

            assertThat(response.brand()).isNull();
            verifyNoInteractions(brandRepository);
        }

        @Test
        void rejectsDuplicateSkuBeforeLookingUpReferences() {
            when(productRepository.existsBySku("RC-AD-15")).thenReturn(true);

            assertThatThrownBy(() -> productService.create(createRequest("rc-ad-15", null, Set.of(DOG))))
                    .isInstanceOf(ProductAlreadyExistsException.class)
                    .hasMessage("Ya existe un producto con el SKU 'RC-AD-15'");
            verifyNoInteractions(categoryRepository, speciesService);
        }

        @Test
        void rejectsUnknownCategory() {
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(createRequest("RC-1", null, Set.of(DOG))))
                    .isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        void rejectsInactiveCategory() {
            food.deactivate();
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));

            assertThatThrownBy(() -> productService.create(createRequest("RC-1", null, Set.of(DOG))))
                    .isInstanceOf(InactiveCategoryException.class)
                    .hasMessage("La categoría 'Alimento' está desactivada");
        }

        @Test
        void rejectsInactiveBrand() {
            royalCanin.deactivate();
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));
            when(brandRepository.findById(ROYAL_CANIN_ID)).thenReturn(Optional.of(royalCanin));

            assertThatThrownBy(() -> productService.create(createRequest("RC-1", ROYAL_CANIN_ID, Set.of(DOG))))
                    .isInstanceOf(InactiveBrandException.class);
        }

        @Test
        void propagatesSpeciesValidationErrorAndDoesNotSave() {
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));
            doThrow(new InactiveSpeciesException("Gato")).when(speciesService).assertActiveSpecies(anyCollection());

            assertThatThrownBy(() -> productService.create(createRequest("RC-1", null, Set.of(CAT))))
                    .isInstanceOf(InactiveSpeciesException.class);
            verify(productRepository, never()).save(any());
        }

    }

    @Nested
    class Update {

        private Product product;

        @BeforeEach
        void setUpProduct() {
            product = new Product("RC-AD-15", "Adulto 15 kg", null, food, royalCanin,
                    Money.of(new BigDecimal("189.90"), Money.PEN), 10, null, Set.of(DOG));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        }

        @Test
        void keepingCategoryAndBrandDoesNotLookThemUpAgainWhenProductStaysInactive() {
            ProductResponse response = productService.update(PRODUCT_ID,
                    updateRequest(FOOD_ID, ROYAL_CANIN_ID, Set.of(DOG), "199.90", false));

            assertThat(response.price()).isEqualByComparingTo("199.90");
            assertThat(response.active()).isFalse();
            assertThat(response.stock()).isEqualTo(10);
            verifyNoInteractions(categoryRepository, brandRepository, speciesService);
            verify(productRepository).flush();
        }

        @Test
        void cannotStayActiveInADeactivatedCategory() {
            food.deactivate();
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));

            assertThatThrownBy(() -> productService.update(PRODUCT_ID,
                    updateRequest(FOOD_ID, ROYAL_CANIN_ID, Set.of(DOG), "189.90", true)))
                    .isInstanceOf(InactiveCategoryException.class);
        }

        @Test
        void canMoveToAnotherActiveCategory() {
            Category toys = withId(new Category("Juguetes", null), TOYS_ID);
            when(categoryRepository.findById(TOYS_ID)).thenReturn(Optional.of(toys));

            ProductResponse response = productService.update(PRODUCT_ID,
                    updateRequest(TOYS_ID, ROYAL_CANIN_ID, Set.of(DOG), "189.90", true));

            assertThat(response.category().id()).isEqualTo(TOYS_ID);
        }

        @Test
        void keepsItsCurrentBrandEvenIfDeactivated() {
            royalCanin.deactivate();
            when(categoryRepository.findById(FOOD_ID)).thenReturn(Optional.of(food));

            ProductResponse response = productService.update(PRODUCT_ID,
                    updateRequest(FOOD_ID, ROYAL_CANIN_ID, Set.of(DOG), "189.90", true));

            assertThat(response.brand().id()).isEqualTo(ROYAL_CANIN_ID);
            verifyNoInteractions(brandRepository);
        }

        @Test
        void changingToAnInactiveBrandIsRejected() {
            Brand proPlan = withId(new Brand("Pro Plan"), PRO_PLAN_ID);
            proPlan.deactivate();
            when(brandRepository.findById(PRO_PLAN_ID)).thenReturn(Optional.of(proPlan));

            assertThatThrownBy(() -> productService.update(PRODUCT_ID,
                    updateRequest(FOOD_ID, PRO_PLAN_ID, Set.of(DOG), "189.90", false)))
                    .isInstanceOf(InactiveBrandException.class);
        }

        @Test
        void brandCanBeRemoved() {
            ProductResponse response = productService.update(PRODUCT_ID,
                    updateRequest(FOOD_ID, null, Set.of(DOG), "189.90", false));

            assertThat(response.brand()).isNull();
        }

        @Test
        void onlyAddedSpeciesAreValidated() {
            ProductResponse response = productService.update(PRODUCT_ID,
                    updateRequest(FOOD_ID, ROYAL_CANIN_ID, Set.of(DOG, CAT), "189.90", false));

            verify(speciesService).assertActiveSpecies(Set.of(CAT));
            assertThat(response.speciesIds()).containsExactlyInAnyOrder(DOG, CAT);
        }

        @Test
        void rejectsSkuUsedByAnotherProductWithoutModifyingIt() {
            when(productRepository.existsBySkuAndIdNot("PP-1", PRODUCT_ID)).thenReturn(true);

            assertThatThrownBy(() -> productService.update(PRODUCT_ID,
                    new UpdateProductRequest("pp-1", "Otro", null, FOOD_ID, null, new BigDecimal("5"), null,
                            Set.of(DOG), false)))
                    .isInstanceOf(ProductAlreadyExistsException.class);
            assertThat(product.getSku()).isEqualTo("RC-AD-15");
        }

    }

    @Nested
    class StockAndLifecycle {

        @Test
        void adjustStockAppliesDeltaAndFlushes() {
            Product product = productWithStock(10);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductResponse response = productService.adjustStock(PRODUCT_ID, new AdjustStockRequest(-4));

            assertThat(response.stock()).isEqualTo(6);
            verify(productRepository).flush();
        }

        @Test
        void adjustStockBelowZeroIsRejected() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(2)));

            assertThatThrownBy(() -> productService.adjustStock(PRODUCT_ID, new AdjustStockRequest(-3)))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        void getByIdThrowsNotFoundForUnknownId() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(PRODUCT_ID))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("No existe un producto con id 50");
        }

        @Test
        void deactivateMarksProductAsInactive() {
            Product product = productWithStock(1);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            productService.deactivate(PRODUCT_ID);

            assertThat(product.isActive()).isFalse();
        }

    }

    // --- utilidades ---

    private static CreateProductRequest createRequest(String sku, Long brandId, Set<Long> speciesIds) {
        return new CreateProductRequest(sku, "Adulto 15 kg", null, FOOD_ID, brandId,
                new BigDecimal("189.90"), 20, null, speciesIds);
    }

    private static UpdateProductRequest updateRequest(Long categoryId, Long brandId, Set<Long> speciesIds,
                                                      String price, boolean active) {
        return new UpdateProductRequest("RC-AD-15", "Adulto 15 kg", null, categoryId, brandId,
                new BigDecimal(price), null, speciesIds, active);
    }

    private Product productWithStock(int stock) {
        return new Product("RC-AD-15", "Adulto 15 kg", null, food, null,
                Money.of(new BigDecimal("189.90"), Money.PEN), stock, null, Set.of(DOG));
    }

    /** Simula una entidad ya persistida: el id lo asigna la base de datos. */
    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

}
