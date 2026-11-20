package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer administrador al arrancar si no existe ningún usuario con el rol ADMIN.
 * <p>
 * Se ejecuta una vez por arranque, después de Flyway y de crear todos los beans. Es idempotente:
 * en cuanto hay un admin no hace nada, así que las variables de entorno se pueden quitar después.
 * <ul>
 *   <li>Sin admin y con {@code ADMIN_EMAIL}/{@code ADMIN_PASSWORD}: lo crea.</li>
 *   <li>Sin admin, sin variables y {@code plutimi.admin.required=true} (prod): no arranca.</li>
 *   <li>Sin admin, sin variables y no obligatorio (dev, test): solo avisa en el log.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AdminInitializer implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserService userService;
    private final AdminProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (userService.adminExists()) {
            return;
        }
        if (!properties.isConfigured()) {
            if (properties.required()) {
                throw new IllegalStateException(
                        "No ADMIN user exists: set ADMIN_EMAIL and ADMIN_PASSWORD to create the first one");
            }
            log.warn("No ADMIN user exists and ADMIN_EMAIL/ADMIN_PASSWORD are not set: skipping admin creation");
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        UserResponse admin = userService.create(
                new NewUser(properties.email(), properties.password(),
                        properties.firstName(), properties.lastName(), null),
                Role.ADMIN_CODE);
        log.info("Initial ADMIN user created: {}", admin.email());
    }

}
