package dev.andymallcco.plutimi.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Comprueba las anotaciones de Bean Validation directamente, sin Spring. */
class CreateRoleRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsLowercaseCodeThatIsStoredUppercase() {
        assertThat(invalidFields(new CreateRoleRequest("stock_manager2", "Encargado", null))).isEmpty();
    }

    @Test
    void blankCodeGivesASingleError() {
        // El patrón acepta el blanco: solo falla @NotBlank, no también @Pattern
        assertThat(invalidFields(new CreateRoleRequest("   ", "Encargado", null))).containsExactly("code");
    }

    @Test
    void rejectsCodeStartingWithADigit() {
        assertThat(invalidFields(new CreateRoleRequest("1STOCK", "Encargado", null))).containsExactly("code");
    }

    @Test
    void rejectsCodeWithHyphensOrSpaces() {
        assertThat(invalidFields(new CreateRoleRequest("STOCK-MANAGER", "Encargado", null))).containsExactly("code");
        assertThat(invalidFields(new CreateRoleRequest("STOCK MANAGER", "Encargado", null))).containsExactly("code");
    }

    @Test
    void rejectsTooLongCode() {
        assertThat(invalidFields(new CreateRoleRequest("A".repeat(31), "Encargado", null))).containsExactly("code");
    }

    @Test
    void rejectsBlankName() {
        assertThat(invalidFields(new CreateRoleRequest("STOCK_MANAGER", "  ", null))).containsExactly("name");
    }

    /** Una entrada por violación: un campo repetido significa dos errores para el mismo campo. */
    private List<String> invalidFields(CreateRoleRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }

}
