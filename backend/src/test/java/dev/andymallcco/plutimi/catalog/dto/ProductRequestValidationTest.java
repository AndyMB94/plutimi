package dev.andymallcco.plutimi.catalog.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reglas de Bean Validation de los requests de productos, sin Spring. */
class ProductRequestValidationTest {

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

    // --- CreateProductRequest ---

    @Test
    void acceptsValidCreateRequestWithoutOptionalFields() {
        assertThat(invalidFields(create("RC-AD-15", "189.90", Set.of(1L)))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RC AD", "-RC", "RC-", "RC--AD", "RC_AD", "ÑU-1"})
    void rejectsMalformedSku(String sku) {
        assertThat(invalidFields(create(sku, "189.90", Set.of(1L)))).containsExactly("sku");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "10.999", "12345678901"})
    void rejectsInvalidPrice(String price) {
        assertThat(invalidFields(create("RC-1", price, Set.of(1L)))).containsExactly("price");
    }

    @Test
    void requiresAtLeastOneSpecies() {
        assertThat(invalidFields(create("RC-1", "10", Set.of()))).containsExactly("speciesIds");
    }

    @Test
    void rejectsNonPositiveSpeciesId() {
        assertThat(invalidFields(create("RC-1", "10", Set.of(0L))))
                .singleElement().asString().startsWith("speciesIds");
    }

    @Test
    void rejectsImageUrlThatIsNotHttp() {
        CreateProductRequest request = new CreateProductRequest("RC-1", "Snack", null, 1L, null,
                new BigDecimal("10"), 0, "ftp://example.com/a.jpg", Set.of(1L));

        assertThat(invalidFields(request)).containsExactly("imageUrl");
    }

    @Test
    void rejectsNegativeStock() {
        CreateProductRequest request = new CreateProductRequest("RC-1", "Snack", null, 1L, null,
                new BigDecimal("10"), -1, null, Set.of(1L));

        assertThat(invalidFields(request)).containsExactly("stock");
    }

    // --- AdjustStockRequest ---

    @Test
    void acceptsPositiveAndNegativeDelta() {
        assertThat(invalidFields(new AdjustStockRequest(5))).isEmpty();
        assertThat(invalidFields(new AdjustStockRequest(-5))).isEmpty();
    }

    @Test
    void rejectsZeroDelta() {
        assertThat(invalidFields(new AdjustStockRequest(0))).containsExactly("deltaNonZero");
    }

    @Test
    void rejectsMissingDeltaOnlyOnce() {
        assertThat(invalidFields(new AdjustStockRequest(null))).containsExactly("delta");
    }

    @Test
    void rejectsDeltaOutOfRange() {
        assertThat(invalidFields(new AdjustStockRequest(1_000_001))).containsExactly("delta");
    }

    // --- utilidades ---

    private static CreateProductRequest create(String sku, String price, Set<Long> speciesIds) {
        return new CreateProductRequest(sku, "Adulto 15 kg", null, 1L, null, new BigDecimal(price), 10, null, speciesIds);
    }

    private static <T> Set<String> invalidFields(T request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

}
