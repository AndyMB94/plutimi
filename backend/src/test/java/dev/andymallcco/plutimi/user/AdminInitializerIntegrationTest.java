package dev.andymallcco.plutimi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.andymallcco.plutimi.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link AdminInitializer} contra PostgreSQL. Se instancia a mano con distintas propiedades; el
 * bean del contexto ya se ejecutó al arrancar sin variables y no creó nada.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AdminInitializerIntegrationTest {

    private static final String PASSWORD = "a-strong-admin-password";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createsTheAdminWhenNoneExistsAndIsIdempotent() {
        AdminInitializer initializer = initializer(new AdminProperties("Root@Plutimi.dev", PASSWORD, "Admin", "Root", true));

        run(initializer);
        run(initializer);

        assertThat(userRepository.findAll())
                .singleElement()
                .satisfies(admin -> {
                    assertThat(admin.getEmail()).isEqualTo("root@plutimi.dev");
                    assertThat(admin.getPasswordHash()).startsWith("$2a$10$").isNotEqualTo(PASSWORD);
                });
        assertThat(userService.adminExists()).isTrue();
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        userService.create(new NewUser("lucia@plutimi.dev", PASSWORD, "Lucía", "Ramos", null), Role.ADMIN_CODE);

        run(initializer(new AdminProperties("root@plutimi.dev", PASSWORD, "Admin", "Root", true)));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void failsWhenRequiredAndNotConfigured() {
        AdminInitializer initializer = initializer(new AdminProperties("", "", "Admin", "Root", true));

        assertThatThrownBy(() -> run(initializer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL and ADMIN_PASSWORD");
    }

    @Test
    void onlySkipsWhenNotRequiredAndNotConfigured() {
        run(initializer(new AdminProperties(null, null, "Admin", "Root", false)));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsAShortPassword() {
        AdminInitializer initializer = initializer(new AdminProperties("root@plutimi.dev", "short", "Admin", "Root", false));

        assertThatThrownBy(() -> run(initializer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
        assertThat(userRepository.count()).isZero();
    }

    private AdminInitializer initializer(AdminProperties properties) {
        return new AdminInitializer(userService, properties);
    }

    private static void run(AdminInitializer initializer) {
        initializer.run(new DefaultApplicationArguments());
    }

}
