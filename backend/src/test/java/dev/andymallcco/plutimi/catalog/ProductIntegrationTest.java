package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.andymallcco.plutimi.TestcontainersConfiguration;
import dev.andymallcco.plutimi.catalog.dto.CreateBrandRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateCategoryRequest;
import dev.andymallcco.plutimi.catalog.dto.CreateProductRequest;
import dev.andymallcco.plutimi.species.SpeciesService;
import dev.andymallcco.plutimi.species.dto.SpeciesResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Productos de punta a punta por HTTP contra PostgreSQL: binding de parámetros, validación,
 * códigos de estado y formato JSON. Las combinaciones de filtros se prueban a fondo en
 * ProductSpecificationsIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ProductIntegrationTest {

    private static final String PRODUCTS = "/api/v1/products";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SpeciesService speciesService;

    private long dog;
    private long cat;
    private long food;
    private long royalCanin;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        List<SpeciesResponse> species = speciesService.listSpecies(false, PageRequest.of(0, 2, Sort.by("id"))).content();
        dog = species.get(0).id();
        cat = species.get(1).id();
        food = categoryService.create(new CreateCategoryRequest("Alimento", null)).id();
        royalCanin = brandService.create(new CreateBrandRequest("Royal Canin")).id();
    }

    // --- crear ---

    @Test
    void createReturns201WithLocationAndFullDetail() throws Exception {
        MvcTestResult result = mvc.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sku": "rc-ad-15",
                          "name": "Adulto 15 kg",
                          "categoryId": %d,
                          "brandId": %d,
                          "price": 189.9,
                          "stock": 20,
                          "speciesIds": [%d]
                        }
                        """.formatted(food, royalCanin, dog))
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "sku": "RC-AD-15",
                          "slug": "adulto-15-kg-rc-ad-15",
                          "price": 189.90,
                          "currency": "PEN",
                          "stock": 20,
                          "active": true,
                          "category": { "name": "Alimento", "slug": "alimento" },
                          "brand": { "name": "Royal Canin", "slug": "royal-canin" },
                          "speciesIds": [%d]
                        }
                        """.formatted(dog));
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).endsWith(PRODUCTS + "/" + idFrom(result));
        assertThat(mvc.get().uri(location)).hasStatusOk();
    }

    @Test
    void invalidBodyReturns400WithEveryInvalidField() {
        assertThat(mvc.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "sku": "RC AD", "name": "Adulto", "categoryId": %d, "price": 0, "stock": 1, "speciesIds": [] }
                        """.formatted(food)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("price", "sku", "speciesIds");
    }

    @Test
    void unknownCategoryReturns404() {
        assertThat(mvc.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("RC-1", 999_999L, dog)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "No existe una categoría con id 999999" }
                        """);
    }

    @Test
    void inactiveCategoryReturns422() {
        long toys = categoryService.create(new CreateCategoryRequest("Juguetes", null)).id();
        categoryService.deactivate(toys);

        assertThat(mvc.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("RC-1", toys, dog)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "La categoría 'Juguetes' está desactivada" }
                        """);
    }

    @Test
    void unknownSpeciesReturns404() {
        assertThat(mvc.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("RC-1", food, 999_999L)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void duplicateSkuIgnoringCaseReturns409() {
        createProduct("RC-AD-15", 10);

        assertThat(mvc.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("rc-ad-15", food, dog)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Ya existe un producto con el SKU 'RC-AD-15'" }
                        """);
    }

    // --- listar ---

    @Test
    void listBindsFilterParametersFromTheQueryString() {
        createProduct("RC-DOG", 10, "50.00", Set.of(dog));
        createProduct("RC-CAT", 10, "45.00", Set.of(cat));

        assertThat(mvc.get().uri(PRODUCTS + "?speciesId={species}&maxPrice=46", cat))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[*].sku").asArray().containsExactly("RC-CAT");
    }

    @Test
    void listReturnsSummariesWithoutDescriptionOrSpecies() {
        createProduct("RC-DOG", 10);

        assertThat(mvc.get().uri(PRODUCTS))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0]").asMap()
                .containsKeys("sku", "price", "brand", "categoryId")
                .doesNotContainKeys("description", "speciesIds");
    }

    @Test
    void invertedPriceRangeReturns400() {
        assertThat(mvc.get().uri(PRODUCTS + "?minPrice=50&maxPrice=10"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errors": [ { "field": "priceRangeValid", "message": "El precio mínimo no puede ser mayor que el máximo" } ] }
                        """);
    }

    @Test
    void unparsableFilterValueReturns400InSpanish() {
        assertThat(mvc.get().uri(PRODUCTS + "?minPrice=abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errors": [ { "field": "minPrice", "message": "El valor 'abc' no tiene un formato válido" } ] }
                        """);
    }

    // --- reemplazar, stock y desactivar ---

    @Test
    void putReplacesProductButKeepsStock() {
        long id = createProduct("RC-AD-15", 7);

        assertThat(mvc.put().uri(PRODUCTS + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sku": "RC-AD-15",
                          "name": "Adulto 15 kg",
                          "categoryId": %d,
                          "price": 199.90,
                          "speciesIds": [%d, %d],
                          "active": true
                        }
                        """.formatted(food, dog, cat)))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "price": 199.90, "stock": 7, "brand": null }
                        """);
    }

    @Test
    void patchStockAppliesDelta() {
        long id = createProduct("RC-AD-15", 7);

        assertThat(patchStock(id, -2)).hasStatusOk().bodyJson().extractingPath("$.stock").isEqualTo(5);
        assertThat(patchStock(id, 10)).hasStatusOk().bodyJson().extractingPath("$.stock").isEqualTo(15);
    }

    @Test
    void patchStockBelowZeroReturns422AndKeepsStock() {
        long id = createProduct("RC-AD-15", 2);

        assertThat(patchStock(id, -3))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Stock insuficiente para 'Producto RC-AD-15': hay 2 unidades y se solicitaron 3" }
                        """);
        assertThat(productService.getById(id).stock()).isEqualTo(2);
    }

    @Test
    void patchStockWithZeroDeltaReturns400() {
        long id = createProduct("RC-AD-15", 2);

        assertThat(patchStock(id, 0))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errors": [ { "field": "deltaNonZero" } ] }
                        """);
    }

    @Test
    void deleteDeactivatesAndHidesFromCatalog() {
        long id = createProduct("RC-AD-15", 2);

        assertThat(mvc.delete().uri(PRODUCTS + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().uri(PRODUCTS + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(PRODUCTS)).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
        assertThat(mvc.get().uri(PRODUCTS + "/{id}", id)).bodyJson().extractingPath("$.active").isEqualTo(false);
    }

    @Test
    void getByUnknownIdReturns404() {
        assertThat(mvc.get().uri(PRODUCTS + "/999999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "No existe un producto con id 999999" }
                        """);
    }

    // --- utilidades ---

    private MvcTestResult patchStock(long id, int delta) {
        return mvc.patch().uri(PRODUCTS + "/{id}/stock", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "delta": %d }
                        """.formatted(delta))
                .exchange();
    }

    private long createProduct(String sku, int stock) {
        return createProduct(sku, stock, "10.00", Set.of(dog));
    }

    private long createProduct(String sku, int stock, String price, Set<Long> speciesIds) {
        return productService.create(new CreateProductRequest(sku, "Producto " + sku, null, food, royalCanin,
                new BigDecimal(price), stock, null, speciesIds)).id();
    }

    private static String productJson(String sku, long categoryId, long speciesId) {
        return """
                { "sku": "%s", "name": "Producto", "categoryId": %d, "price": 10, "stock": 1, "speciesIds": [%d] }
                """.formatted(sku, categoryId, speciesId);
    }

    private static long idFrom(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

}
