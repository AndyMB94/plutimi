package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.andymallcco.plutimi.TestcontainersConfiguration;
import dev.andymallcco.plutimi.catalog.dto.CreateCategoryRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Aplicación completa contra un PostgreSQL real (Testcontainers): HTTP → controller → service →
 * repositorio → base de datos, con Flyway, auditoría, paginación y CORS reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CategoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final String CATEGORIES = "/api/v1/categories";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        categoryRepository.deleteAll();
    }

    // --- crear y obtener ---

    @Test
    void createReturns201WithLocationAndAuditDates() throws Exception {
        MvcTestResult result = mvc.post().uri(CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Alimento para Perros", "description": "Croquetas" }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "name": "Alimento para Perros",
                          "slug": "alimento-para-perros",
                          "description": "Croquetas",
                          "active": true,
                          "createdAt": "2026-10-29T10:00:00Z",
                          "updatedAt": "2026-10-29T10:00:00Z"
                        }
                        """);
        long id = idFrom(result);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(CATEGORIES + "/" + id);
    }

    @Test
    void getByIdReturnsCategory() {
        long id = createCategory("Juguetes");

        assertThat(mvc.get().uri(CATEGORIES + "/{id}", id))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "name": "Juguetes", "slug": "juguetes", "active": true }
                        """);
    }

    @Test
    void getByUnknownIdReturns404() {
        assertThat(mvc.get().uri(CATEGORIES + "/999999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "No existe una categoría con id 999999" }
                        """);
    }

    @Test
    void nonPositiveIdReturns400() {
        assertThat(mvc.get().uri(CATEGORIES + "/0"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errors": [ { "field": "id" } ] }
                        """);
    }

    @Test
    void duplicateNameIgnoringCaseReturns409() {
        createCategory("Juguetes");

        assertThat(mvc.post().uri(CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "JUGUETES" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Ya existe una categoría con el nombre 'JUGUETES'" }
                        """);
    }

    // --- listar ---

    @Test
    void listReturnsOnlyActiveCategoriesSortedByName() {
        createCategory("Snacks");
        createCategory("Accesorios");
        long toys = createCategory("Juguetes");
        categoryService.deactivate(toys);

        MvcTestResult result = mvc.get().uri(CATEGORIES).exchange();

        assertThat(result).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Accesorios", "Snacks");
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                { "page": 0, "size": 20, "totalElements": 2, "totalPages": 1 }
                """);
    }

    @Test
    void listIncludesInactiveCategoriesWhenRequested() {
        createCategory("Snacks");
        categoryService.deactivate(createCategory("Juguetes"));

        assertThat(mvc.get().uri(CATEGORIES + "?includeInactive=true"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void pageSizeIsCappedAt100() {
        assertThat(mvc.get().uri(CATEGORIES + "?size=500"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.size").isEqualTo(100);
    }

    @Test
    void invalidSortPropertyReturns400() {
        createCategory("Juguetes");

        assertThat(mvc.get().uri(CATEGORIES + "?sort=unknown"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/invalid-sort",
                          "detail": "No se puede ordenar por 'unknown'."
                        }
                        """);
    }

    // --- actualizar y desactivar ---

    @Test
    void updateReplacesCategory() {
        long id = createCategory("Juguetes");

        assertThat(mvc.put().uri(CATEGORIES + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Juguetes y Accesorios", "description": "Pelotas", "active": false }
                        """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "name": "Juguetes y Accesorios",
                          "slug": "juguetes-y-accesorios",
                          "description": "Pelotas",
                          "active": false
                        }
                        """);
    }

    @Test
    void updateWithoutActiveFlagReturns400() {
        long id = createCategory("Juguetes");

        assertThat(mvc.put().uri(CATEGORIES + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Juguetes" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errors": [ { "field": "active" } ] }
                        """);
    }

    @Test
    void deleteDeactivatesAndIsIdempotent() {
        long id = createCategory("Juguetes");

        assertThat(mvc.delete().uri(CATEGORIES + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().uri(CATEGORIES + "/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(CATEGORIES + "/{id}", id))
                .hasStatusOk()
                .bodyJson().extractingPath("$.active").isEqualTo(false);
    }

    // --- CORS ---

    @Test
    void corsPreflightFromFrontendIsAllowed() {
        assertThat(mvc.options().uri(CATEGORIES)
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .hasStatusOk()
                .headers()
                .hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200")
                .hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void corsPreflightFromUnknownOriginIsRejected() {
        assertThat(mvc.options().uri(CATEGORIES)
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    // --- utilidades ---

    private long createCategory(String name) {
        return categoryService.create(new CreateCategoryRequest(name, null)).id();
    }

    private static long idFrom(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Reloj fijo para comprobar las fechas de auditoría. @Primary: se usa en lugar del Clock de la aplicación. */
    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

}
