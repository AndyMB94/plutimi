package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.andymallcco.plutimi.TestcontainersConfiguration;
import dev.andymallcco.plutimi.catalog.dto.CreateBrandRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Marcas de punta a punta contra PostgreSQL. La paginación, la auditoría y CORS ya se prueban
 * en CategoryIntegrationTest; aquí solo lo propio de las marcas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class BrandIntegrationTest {

    private static final String BRANDS = "/api/v1/brands";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    @BeforeEach
    void cleanDatabase() {
        brandRepository.deleteAll();
    }

    @Test
    void createReturns201WithLocationThatPointsToTheBrand() throws Exception {
        MvcTestResult result = mvc.post().uri(BRANDS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Royal Canin" }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "name": "Royal Canin", "slug": "royal-canin", "active": true }
                        """);
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).endsWith(BRANDS + "/" + idFrom(result));

        assertThat(mvc.get().uri(location)).hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Royal Canin");
    }

    @Test
    void blankNameReturns400() {
        // "  " incumple @NotBlank y también @Pattern (no tiene letras ni dígitos): dos errores del mismo campo
        assertThat(mvc.post().uri(BRANDS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "  " }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors[*].field").asArray().containsOnly("name");
    }

    @Test
    void duplicateNameIgnoringCaseReturns409() {
        createBrand("Royal Canin");

        assertThat(mvc.post().uri(BRANDS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "ROYAL CANIN" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Ya existe una marca con el nombre 'ROYAL CANIN'" }
                        """);
    }

    @Test
    void getByUnknownIdReturns404() {
        assertThat(mvc.get().uri(BRANDS + "/999999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "No existe una marca con id 999999" }
                        """);
    }

    @Test
    void listReturnsOnlyActiveBrandsSortedByName() {
        createBrand("Royal Canin");
        createBrand("Hill's");
        brandService.deactivate(createBrand("Pro Plan"));

        assertThat(mvc.get().uri(BRANDS))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Hill's", "Royal Canin");
    }

    @Test
    void updateCanRenameAndReactivate() {
        long id = createBrand("Pro Plan");
        brandService.deactivate(id);

        assertThat(mvc.put().uri(BRANDS + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Purina Pro Plan", "active": true }
                        """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "name": "Purina Pro Plan", "slug": "purina-pro-plan", "active": true }
                        """);
    }

    @Test
    void deleteDeactivatesAndIsIdempotent() {
        long id = createBrand("Royal Canin");

        assertThat(mvc.delete().uri(BRANDS + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().uri(BRANDS + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(BRANDS + "/{id}", id))
                .hasStatusOk()
                .bodyJson().extractingPath("$.active").isEqualTo(false);
    }

    private long createBrand(String name) {
        return brandService.create(new CreateBrandRequest(name)).id();
    }

    private static long idFrom(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

}
