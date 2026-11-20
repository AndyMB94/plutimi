package dev.andymallcco.plutimi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.andymallcco.plutimi.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Usuarios de punta a punta contra PostgreSQL. {@link RecordApplicationEvents} graba los eventos
 * publicados durante cada test para comprobarlos sin tener todavía un listener (llega en la 1.7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class UserIntegrationTest {

    private static final String USERS = "/api/v1/users";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void listFiltersByRoleAndByTextAndIsSortedByEmail() {
        createUser("lucia@plutimi.dev", "Lucía", "Ramos", "ADMIN");
        createUser("diego@plutimi.dev", "Diego", "Quispe", "VET");
        createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThat(mvc.get().uri(USERS))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[*].email").asArray()
                .containsExactly("carla@plutimi.dev", "diego@plutimi.dev", "lucia@plutimi.dev");
        assertThat(mvc.get().uri(USERS + "?roleId={roleId}", roleId("VET")))
                .bodyJson().extractingPath("$.content[*].email").asArray().containsExactly("diego@plutimi.dev");
        assertThat(mvc.get().uri(USERS + "?q={q}", "RAMOS"))
                .bodyJson().extractingPath("$.content[*].email").asArray().containsExactly("lucia@plutimi.dev");
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() {
        createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThat(mvc.get().uri(USERS + "?q={q}", "%"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content").asArray().isEmpty();
    }

    @Test
    void getByIdIncludesTheRoleAndNeverThePasswordHash() {
        long id = createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThat(mvc.get().uri(USERS + "/{id}", id))
                .hasStatusOk()
                .bodyJson()
                .doesNotHavePath("$.passwordHash")
                .isLenientlyEqualTo("""
                        { "email": "carla@plutimi.dev", "role": { "code": "CUSTOMER" }, "enabled": true }
                        """);
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThatThrownBy(() -> createUser("CARLA@plutimi.dev", "Otra", "Persona", "CUSTOMER"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void changeRolePublishesUserRoleChangedEvent() {
        long id = createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThat(mvc.patch().uri(USERS + "/{id}/role", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "roleId": %d }
                        """.formatted(roleId("VET"))))
                .hasStatusOk()
                .bodyJson().extractingPath("$.role.code").isEqualTo("VET");
        assertThat(events.stream(UserRoleChangedEvent.class)).containsExactly(new UserRoleChangedEvent(id));
    }

    @Test
    void demotingTheLastActiveAdminReturns409() {
        long id = createUser("lucia@plutimi.dev", "Lucía", "Ramos", "ADMIN");

        assertThat(mvc.patch().uri(USERS + "/{id}/role", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "roleId": %d }
                        """.formatted(roleId("CUSTOMER"))))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "detail": "Es el último administrador activo: asigna el rol ADMIN a otro usuario antes" }
                        """);
        assertThat(events.stream(UserRoleChangedEvent.class)).isEmpty();
    }

    @Test
    void disablingTheLastActiveAdminReturns409ButAnotherAdminCanBeDisabled() {
        long first = createUser("lucia@plutimi.dev", "Lucía", "Ramos", "ADMIN");

        assertThat(patchStatus(first, false)).hasStatus(HttpStatus.CONFLICT);

        long second = createUser("mario@plutimi.dev", "Mario", "Flores", "ADMIN");
        assertThat(patchStatus(second, false))
                .hasStatusOk()
                .bodyJson().extractingPath("$.enabled").isEqualTo(false);
        // Ahora Lucía vuelve a ser la única administradora activa
        assertThat(patchStatus(first, false)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void disablingAUserPublishesUserDisabledEvent() {
        long id = createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThat(patchStatus(id, false)).hasStatusOk();
        assertThat(events.stream(UserDisabledEvent.class)).containsExactly(new UserDisabledEvent(id));
    }

    @Test
    void missingStatusReturns400() {
        long id = createUser("carla@plutimi.dev", "Carla", "Mendoza", "CUSTOMER");

        assertThat(mvc.patch().uri(USERS + "/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("enabled");
    }

    private MockMvcTester.MockMvcRequestBuilder patchStatus(long id, boolean enabled) {
        return mvc.patch().uri(USERS + "/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "enabled": %s }
                        """.formatted(enabled));
    }

    private long createUser(String email, String firstName, String lastName, String roleCode) {
        return userService.create(new NewUser(email, "secret-password", firstName, lastName, null), roleCode).id();
    }

    private long roleId(String code) {
        return roleRepository.findByCode(code).orElseThrow().getId();
    }

}
