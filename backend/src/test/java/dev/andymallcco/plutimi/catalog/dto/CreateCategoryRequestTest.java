package dev.andymallcco.plutimi.catalog.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Comprueba las anotaciones de Bean Validation directamente, sin Spring. */
class CreateCategoryRequestTest {

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
    void acceptsValidRequest() {
        assertThat(validator.validate(new CreateCategoryRequest("Ñandú y aves", null))).isEmpty();
    }

    @Test
    void rejectsBlankName() {
        assertThat(invalidFields(new CreateCategoryRequest("   ", null))).contains("name");
    }

    @Test
    void rejectsNameWithoutLettersOrDigits() {
        assertThat(invalidFields(new CreateCategoryRequest("¡¡!!", null))).containsExactly("name");
    }

    @Test
    void rejectsTooLongName() {
        assertThat(invalidFields(new CreateCategoryRequest("a".repeat(101), null))).containsExactly("name");
    }

    @Test
    void rejectsTooLongDescription() {
        assertThat(invalidFields(new CreateCategoryRequest("Juguetes", "a".repeat(1001))))
                .containsExactly("description");
    }

    private Set<String> invalidFields(CreateCategoryRequest request) {
        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

}
