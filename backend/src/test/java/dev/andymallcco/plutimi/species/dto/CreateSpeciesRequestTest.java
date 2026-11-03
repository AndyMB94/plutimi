package dev.andymallcco.plutimi.species.dto;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprueba las anotaciones de Bean Validation directamente, sin Spring. */
class CreateSpeciesRequestTest {

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

    @ParameterizedTest
    @ValueSource(strings = {"DOG", "GUINEA_PIG", "A"})
    void acceptsValidCodes(String code) {
        assertThat(validator.validate(new CreateSpeciesRequest(code, "Cuy"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dog", "GUINEA PIG", "_DOG", "DOG1", "ÑANDU"})
    void rejectsCodesOutsideTheFormat(String code) {
        assertThat(invalidFields(new CreateSpeciesRequest(code, "Cuy"))).containsExactly("code");
    }

    @Test
    void rejectsTooLongCode() {
        assertThat(invalidFields(new CreateSpeciesRequest("A".repeat(31), "Cuy"))).containsExactly("code");
    }

    @Test
    void rejectsNameWithoutLetters() {
        assertThat(invalidFields(new CreateSpeciesRequest("GUINEA_PIG", "123"))).containsExactly("name");
    }

    @Test
    void rejectsTooLongName() {
        assertThat(invalidFields(new CreateSpeciesRequest("GUINEA_PIG", "a".repeat(61)))).containsExactly("name");
    }

    private Set<String> invalidFields(CreateSpeciesRequest request) {
        Set<ConstraintViolation<CreateSpeciesRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

}
