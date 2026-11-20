package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.andymallcco.plutimi.TestcontainersConfiguration;
import dev.andymallcco.plutimi.catalog.dto.CreateBrandRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateCategoryRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateProductRequest;
import dev.andymallcco.plutimi.catalog.dto.ProductFilter;
import dev.andymallcco.plutimi.catalog.dto.ProductSummaryResponse;
import dev.andymallcco.plutimi.species.SpeciesService;
import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los filtros del catálogo contra PostgreSQL real: las Specifications solo se pueden comprobar
 * de verdad ejecutando el SQL que genera Hibernate.
 * <p>
 * {@code @Transactional}: cada test se deshace al terminar. El borrado inicial de productos
 * también se deshace, así que no afecta a otras clases de test.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class ProductSpecificationsIntegrationTest {

    private static final PageRequest BY_NAME = PageRequest.of(0, 20, Sort.by("name"));

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpeciesService speciesService;

    private long dog;
    private long cat;
    private long food;
    private long toys;
    private long royalCanin;

    /**
     * Catálogo de prueba:
     * <pre>
     * Croquetas Perro  RC-DOG-1   Alimento  Royal Canin  perro        50.00   stock 10
     * Croquetas Gato   RC-CAT-1   Alimento  Royal Canin  gato         45.50   stock 0
     * Pelota 100%      TOY-1      Juguetes  sin marca    perro, gato  15.00   stock 3
     * Rascador         TOY-2      Juguetes  sin marca    gato         80.00   stock 2   (inactivo)
     * </pre>
     */
    @BeforeEach
    void setUpCatalog() {
        productRepository.deleteAll();
        List<SpeciesResponse> species = speciesService.listSpecies(false, PageRequest.of(0, 2, Sort.by("id"))).content();
        dog = species.get(0).id();
        cat = species.get(1).id();
        food = categoryService.create(new CreateCategoryRequest("Alimento test", null)).id();
        toys = categoryService.create(new CreateCategoryRequest("Juguetes test", null)).id();
        royalCanin = brandService.create(new CreateBrandRequest("Royal Canin test")).id();

        createProduct("RC-DOG-1", "Croquetas Perro", food, royalCanin, "50.00", 10, Set.of(dog));
        createProduct("RC-CAT-1", "Croquetas Gato", food, royalCanin, "45.50", 0, Set.of(cat));
        createProduct("TOY-1", "Pelota 100%", toys, null, "15.00", 3, Set.of(dog, cat));
        productService.deactivate(createProduct("TOY-2", "Rascador", toys, null, "80.00", 2, Set.of(cat)));
    }

    @Test
    void withoutFiltersReturnsOnlyActiveProducts() {
        assertThat(names(ProductFilter.none())).containsExactly("Croquetas Gato", "Croquetas Perro", "Pelota 100%");
    }

    @Test
    void includeInactiveReturnsEverything() {
        assertThat(names(filter().includeInactive(true).build())).hasSize(4).contains("Rascador");
    }

    @Test
    void filtersByCategory() {
        assertThat(names(filter().categoryId(toys).build())).containsExactly("Pelota 100%");
    }

    @Test
    void filtersByBrand() {
        assertThat(names(filter().brandId(royalCanin).build())).containsExactly("Croquetas Gato", "Croquetas Perro");
    }

    @Test
    void filtersBySpeciesIncludingProductsForSeveralSpecies() {
        assertThat(names(filter().speciesId(cat).build())).containsExactly("Croquetas Gato", "Pelota 100%");
    }

    @Test
    void searchMatchesNameIgnoringCase() {
        assertThat(names(filter().q("  CROQUETAS ").build())).containsExactly("Croquetas Gato", "Croquetas Perro");
    }

    @Test
    void searchMatchesSku() {
        assertThat(names(filter().q("toy-1").build())).containsExactly("Pelota 100%");
    }

    @Test
    void searchTreatsPercentSignAsTextNotAsWildcard() {
        // Sin escapar, "%" sería un comodín y devolvería todos los productos
        assertThat(names(filter().q("%").build())).containsExactly("Pelota 100%");
    }

    @Test
    void priceRangeIsInclusive() {
        assertThat(names(filter().minPrice("15.00").maxPrice("45.50").build()))
                .containsExactly("Croquetas Gato", "Pelota 100%");
    }

    @Test
    void inStockTrueExcludesSoldOutProducts() {
        assertThat(names(filter().inStock(true).build())).containsExactly("Croquetas Perro", "Pelota 100%");
    }

    @Test
    void inStockFalseReturnsOnlySoldOutProducts() {
        assertThat(names(filter().inStock(false).build())).containsExactly("Croquetas Gato");
    }

    @Test
    void filtersAreCombinedWithAnd() {
        ProductFilter dogFoodInStock = filter().categoryId(food).speciesId(dog).inStock(true).build();

        assertThat(names(dogFoodInStock)).containsExactly("Croquetas Perro");
    }

    @Test
    void canSortByPriceDescending() {
        List<ProductSummaryResponse> page = productService.list(ProductFilter.none(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "price.amount"))).content();

        assertThat(page).extracting(ProductSummaryResponse::name)
                .containsExactly("Croquetas Perro", "Croquetas Gato", "Pelota 100%");
    }

    @Test
    void summaryIncludesBrandAndCategory() {
        ProductSummaryResponse croquetas = productService.list(filter().q("RC-DOG-1").build(), BY_NAME)
                .content().getFirst();

        assertThat(croquetas.brand().name()).isEqualTo("Royal Canin test");
        assertThat(croquetas.categoryId()).isEqualTo(food);
        assertThat(croquetas.price()).isEqualByComparingTo("50.00");
    }

    // --- utilidades ---

    private List<String> names(ProductFilter filter) {
        return productService.list(filter, BY_NAME).content().stream().map(ProductSummaryResponse::name).toList();
    }

    private long createProduct(String sku, String name, long categoryId, Long brandId, String price, int stock,
                               Set<Long> speciesIds) {
        return productService.create(new CreateProductRequest(sku, name, null, categoryId, brandId,
                new BigDecimal(price), stock, null, speciesIds)).id();
    }

    private static FilterBuilder filter() {
        return new FilterBuilder();
    }

    /** Evita escribir los 8 argumentos del record en cada test. */
    private static final class FilterBuilder {
        private Long categoryId;
        private Long brandId;
        private Long speciesId;
        private String q;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Boolean inStock;
        private Boolean includeInactive;

        FilterBuilder categoryId(long value) { categoryId = value; return this; }
        FilterBuilder brandId(long value) { brandId = value; return this; }
        FilterBuilder speciesId(long value) { speciesId = value; return this; }
        FilterBuilder q(String value) { q = value; return this; }
        FilterBuilder minPrice(String value) { minPrice = new BigDecimal(value); return this; }
        FilterBuilder maxPrice(String value) { maxPrice = new BigDecimal(value); return this; }
        FilterBuilder inStock(boolean value) { inStock = value; return this; }
        FilterBuilder includeInactive(boolean value) { includeInactive = value; return this; }

        ProductFilter build() {
            return new ProductFilter(categoryId, brandId, speciesId, q, minPrice, maxPrice, inStock, includeInactive);
        }
    }

}
