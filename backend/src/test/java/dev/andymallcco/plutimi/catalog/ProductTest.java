package dev.andymallcco.plutimi.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.andymallcco.plutimi.shared.money.Money;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Category FOOD = new Category("Alimento", null);
    private static final Brand ROYAL_CANIN = new Brand("Royal Canin");
    private static final long DOG = 1L;
    private static final long CAT = 2L;

    @Nested
    class Creation {

        @Test
        void newProductIsActiveWithNormalizedFields() {
            Product product = new Product(" rc-ad-15 ", "  Adulto   15 kg ", "  ", FOOD, ROYAL_CANIN,
                    soles("189.9"), 10, "", Set.of(DOG));

            assertThat(product.isActive()).isTrue();
            assertThat(product.getSku()).isEqualTo("RC-AD-15");
            assertThat(product.getName()).isEqualTo("Adulto 15 kg");
            assertThat(product.getDescription()).isNull();
            assertThat(product.getImageUrl()).isNull();
            assertThat(product.getPrice()).isEqualTo(soles("189.90"));
            assertThat(product.getStock()).isEqualTo(10);
            assertThat(product.getSpeciesIds()).containsExactly(DOG);
        }

        @Test
        void slugCombinesNameAndSkuSoProductsWithTheSameNameDoNotCollide() {
            Product royalCanin = product("RC-AD-15", "Adulto 15 kg");
            Product proPlan = product("PP-AD-15", "Adulto 15 kg");

            assertThat(royalCanin.getSlug()).isEqualTo("adulto-15-kg-rc-ad-15");
            assertThat(proPlan.getSlug()).isNotEqualTo(royalCanin.getSlug());
        }

        @Test
        void brandIsOptional() {
            Product product = new Product("GEN-01", "Comedero", null, FOOD, null, soles("25"), 0, null, Set.of(DOG, CAT));

            assertThat(product.getBrand()).isNull();
            assertThat(product.getSpeciesIds()).containsExactlyInAnyOrder(DOG, CAT);
        }

        @Test
        void rejectsNegativeInitialStock() {
            assertThatThrownBy(() -> new Product("X-1", "Snack", null, FOOD, null, soles("5"), -1, null, Set.of(DOG)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsMissingCategory() {
            assertThatThrownBy(() -> new Product("X-1", "Snack", null, null, null, soles("5"), 0, null, Set.of(DOG)))
                    .isInstanceOf(NullPointerException.class);
        }

    }

    @Nested
    class Stock {

        @Test
        void decreaseStockSubtractsUnits() {
            Product product = productWithStock(10);

            product.decreaseStock(3);

            assertThat(product.getStock()).isEqualTo(7);
        }

        @Test
        void decreaseStockCanLeaveItAtZero() {
            Product product = productWithStock(3);

            product.decreaseStock(3);

            assertThat(product.getStock()).isZero();
        }

        @Test
        void decreaseStockBeyondAvailableThrowsAndKeepsStock() {
            Product product = productWithStock(2);

            assertThatThrownBy(() -> product.decreaseStock(5))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Stock insuficiente para 'Adulto 15 kg': hay 2 unidades y se solicitaron 5");
            assertThat(product.getStock()).isEqualTo(2);
        }

        @Test
        void increaseStockAddsUnits() {
            Product product = productWithStock(2);

            product.increaseStock(8);

            assertThat(product.getStock()).isEqualTo(10);
        }

        @Test
        void quantitiesMustBePositive() {
            Product product = productWithStock(5);

            assertThatThrownBy(() -> product.decreaseStock(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> product.increaseStock(-1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void adjustStockAddsOrSubtractsByTheSignOfDelta() {
            Product product = productWithStock(5);

            product.adjustStock(4);
            assertThat(product.getStock()).isEqualTo(9);

            product.adjustStock(-9);
            assertThat(product.getStock()).isZero();
        }

        @Test
        void adjustStockBelowZeroThrows() {
            Product product = productWithStock(1);

            assertThatThrownBy(() -> product.adjustStock(-2)).isInstanceOf(InsufficientStockException.class);
        }

        @Test
        void adjustStockRejectsZeroDelta() {
            assertThatThrownBy(() -> productWithStock(1).adjustStock(0)).isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    class PriceAndSpecies {

        @Test
        void changePriceReplacesPrice() {
            Product product = product("RC-AD-15", "Adulto 15 kg");

            product.changePrice(soles("199.90"));

            assertThat(product.getPrice()).isEqualTo(soles("199.90"));
        }

        @Test
        void priceMustNotBeZero() {
            Product product = product("RC-AD-15", "Adulto 15 kg");

            assertThatThrownBy(() -> product.changePrice(soles("0"))).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void changeSpeciesReplacesTheWholeSet() {
            Product product = product("RC-AD-15", "Adulto 15 kg");

            product.changeSpecies(Set.of(CAT, 3L));

            assertThat(product.getSpeciesIds()).containsExactlyInAnyOrder(CAT, 3L);
        }

        @Test
        void aProductMustTargetAtLeastOneSpecies() {
            Product product = product("RC-AD-15", "Adulto 15 kg");

            assertThatThrownBy(() -> product.changeSpecies(Set.of())).isInstanceOf(IllegalArgumentException.class);
            assertThat(product.getSpeciesIds()).containsExactly(DOG);
        }

        @Test
        void speciesIdsCannotBeModifiedFromOutside() {
            Product product = product("RC-AD-15", "Adulto 15 kg");

            assertThatThrownBy(() -> product.getSpeciesIds().add(CAT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

    }

    @Test
    void updateDetailsRegeneratesSlug() {
        Product product = product("RC-AD-15", "Adulto 15 kg");

        product.updateDetails("rc-ad-3", "Adulto 3 kg", "Croquetas", FOOD, null, "https://img.example/rc.jpg");

        assertThat(product.getSlug()).isEqualTo("adulto-3-kg-rc-ad-3");
        assertThat(product.getBrand()).isNull();
        assertThat(product.getDescription()).isEqualTo("Croquetas");
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        Product product = product("RC-AD-15", "Adulto 15 kg");

        product.deactivate();
        assertThat(product.isActive()).isFalse();

        product.activate();
        assertThat(product.isActive()).isTrue();
    }

    // --- utilidades ---

    private static Product product(String sku, String name) {
        return new Product(sku, name, null, FOOD, ROYAL_CANIN, soles("189.90"), 10, null, Set.of(DOG));
    }

    private static Product productWithStock(int stock) {
        return new Product("RC-AD-15", "Adulto 15 kg", null, FOOD, ROYAL_CANIN, soles("189.90"), stock, null, Set.of(DOG));
    }

    private static Money soles(String amount) {
        return Money.of(new BigDecimal(amount), Money.PEN);
    }

}
