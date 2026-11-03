package dev.andymallcco.plutimi.species;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import dev.andymallcco.plutimi.TestcontainersConfiguration;
import dev.andymallcco.plutimi.species.dto.CreateBreedRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplicación completa contra un PostgreSQL real (Testcontainers), con los datos de referencia
 * de la migración V3.
 * <p>
 * {@code @Transactional}: cada test se ejecuta en una transacción que se deshace al terminar, así
 * los datos de referencia quedan intactos para los demás tests (no se pueden borrar con
 * {@code deleteAll()} como en categorías). MockMvc atiende la petición en el mismo hilo, así que
 * el controller y el service participan en esa misma transacción.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SpeciesIntegrationTest {

    private static final String SPECIES = "/api/v1/species";
    private static final String BREEDS = "/api/v1/breeds";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SpeciesService speciesService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- datos de referencia (V3) ---

    @Test
    void seedContainsBaseSpeciesSortedByName() {
        MvcTestResult result = mvc.get().uri(SPECIES).exchange();

        assertThat(result).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Ave", "Conejo", "Gato", "Perro", "Pez", "Reptil", "Roedor");
        // Página de 100 por defecto: los datos de referencia llegan completos en una llamada
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                { "page": 0, "size": 100, "totalElements": 7, "totalPages": 1 }
                """);
    }

    @Test
    void seedContainsDogBreedsIncludingMestizo() {
        MvcTestResult result = mvc.get().uri(SPECIES + "/{id}/breeds", speciesId("DOG")).exchange();

        assertThat(result).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].name").asArray()
                .contains("Mestizo", "Labrador Retriever", "Perro sin pelo del Perú");
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(20);
    }

    @Test
    void everySeedSpeciesHasAGenericBreed() {
        Integer speciesWithoutGenericBreed = jdbcTemplate.queryForObject("""
                select count(*) from species s
                where not exists (select 1 from breeds b
                                  where b.species_id = s.id and b.name in ('Mestizo', 'Común'))
                """, Integer.class);

        assertThat(speciesWithoutGenericBreed).isZero();
    }

    // --- especies ---

    @Test
    void createSpeciesReturns201WithLocation() throws Exception {
        MvcTestResult result = mvc.post().uri(SPECIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "GUINEA_PIG", "name": "Cuy" }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "code": "GUINEA_PIG", "name": "Cuy", "active": true }
                        """);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(SPECIES + "/" + idFrom(result));
    }

    @Test
    void duplicateCodeReturns409() {
        assertThat(mvc.post().uri(SPECIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "DOG", "name": "Can" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Ya existe una especie con el código 'DOG'" }
                        """);
    }

    @Test
    void lowercaseCodeReturns400() {
        assertThat(mvc.post().uri(SPECIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "code": "guinea_pig", "name": "Cuy" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errors": [ { "field": "code" } ] }
                        """);
    }

    @Test
    void deactivatedSpeciesLeavesTheListButKeepsItsBreeds() {
        long dogId = speciesId("DOG");

        assertThat(mvc.delete().uri(SPECIES + "/{id}", dogId)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(SPECIES))
                .bodyJson().extractingPath("$.content[*].code").asArray().doesNotContain("DOG");
        assertThat(mvc.get().uri(SPECIES + "/{id}/breeds", dogId))
                .bodyJson().extractingPath("$.totalElements").isEqualTo(20);
    }

    // --- razas ---

    @Test
    void createBreedReturns201WithLocationUnderBreeds() throws Exception {
        long dogId = speciesId("DOG");

        MvcTestResult result = mvc.post().uri(SPECIES + "/{id}/breeds", dogId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Border Collie" }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "speciesId": %d, "name": "Border Collie", "active": true }
                        """.formatted(dogId));
        long breedId = idFrom(result);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(BREEDS + "/" + breedId);
        assertThat(mvc.get().uri(BREEDS + "/{id}", breedId)).hasStatusOk();
    }

    @Test
    void duplicateBreedInTheSameSpeciesIgnoringCaseReturns409() {
        assertThat(mvc.post().uri(SPECIES + "/{id}/breeds", speciesId("DOG"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "labrador retriever" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Ya existe la raza 'labrador retriever' en la especie 'Perro'" }
                        """);
    }

    @Test
    void sameBreedNameIsAllowedInAnotherSpecies() {
        // "Mestizo" existe en perros, pero no en gatos (que usan "Común")
        assertThat(mvc.post().uri(SPECIES + "/{id}/breeds", speciesId("CAT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Mestizo" }
                        """))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void createBreedInInactiveSpeciesReturns422() {
        long rabbitId = speciesId("RABBIT");
        speciesService.deactivateSpecies(rabbitId);

        assertThat(mvc.post().uri(SPECIES + "/{id}/breeds", rabbitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Cabeza de León" }
                        """))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "type": "https://plutimi.andymallcco.dev/problems/business-rule-violation",
                          "detail": "La especie 'Conejo' está desactivada"
                        }
                        """);
    }

    @Test
    void listBreedsOfUnknownSpeciesReturns404() {
        assertThat(mvc.get().uri(SPECIES + "/999999/breeds"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "No existe una especie con id 999999" }
                        """);
    }

    @Test
    void deactivatedBreedIsHiddenUnlessInactiveAreRequested() {
        long fishId = speciesId("FISH");
        long goldfishId = createBreed(fishId, "Goldfish");

        assertThat(mvc.delete().uri(BREEDS + "/{id}", goldfishId)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().uri(BREEDS + "/{id}", goldfishId)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri(SPECIES + "/{id}/breeds", fishId))
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Común");
        assertThat(mvc.get().uri(SPECIES + "/{id}/breeds?includeInactive=true", fishId))
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Común", "Goldfish");
    }

    @Test
    void updateBreedReplacesNameAndActiveFlag() {
        long breedId = createBreed(speciesId("FISH"), "Goldfish");

        assertThat(mvc.put().uri(BREEDS + "/{id}", breedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Pez dorado", "active": false }
                        """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "name": "Pez dorado", "active": false }
                        """);
    }

    // --- API para otros módulos ---

    @Test
    void assertBreedBelongsToSpeciesRejectsBreedOfAnotherSpecies() {
        long labradorId = jdbcTemplate.queryForObject(
                "select id from breeds where name = 'Labrador Retriever'", Long.class);

        assertThatThrownBy(() -> speciesService.assertBreedBelongsToSpecies(labradorId, speciesId("CAT")))
                .isInstanceOf(BreedSpeciesMismatchException.class);
    }

    // --- restricciones de la base de datos (V2) ---

    @Test
    void databaseRejectsLowercaseSpeciesCode() {
        // Última barrera si alguien inserta sin pasar por la API (por ejemplo, una migración)
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into species (code, name) values ('hamster', 'Hámster')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_species_code_format");
    }

    // --- utilidades ---

    /** Los ids de los datos de referencia pueden variar: se buscan por su código estable. */
    private long speciesId(String code) {
        return jdbcTemplate.queryForObject("select id from species where code = ?", Long.class, code);
    }

    private long createBreed(long speciesId, String name) {
        return speciesService.createBreed(speciesId, new CreateBreedRequest(name)).id();
    }

    private static long idFrom(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

}
