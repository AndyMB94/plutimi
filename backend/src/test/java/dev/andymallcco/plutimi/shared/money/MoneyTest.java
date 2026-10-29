package dev.andymallcco.plutimi.shared.money;

import static dev.andymallcco.plutimi.shared.money.Money.PEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    private static Money pen(String amount) {
        return Money.of(new BigDecimal(amount), PEN);
    }

    @Test
    void normalizesAmountToTwoDecimals() {
        assertThat(pen("10").amount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void roundsHalfUp() {
        assertThat(pen("10.005").amount()).isEqualTo(new BigDecimal("10.01"));
        assertThat(pen("10.004").amount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void isEqualByValueRegardlessOfInputScale() {
        // new BigDecimal("10.0").equals(new BigDecimal("10.00")) es false: la normalización lo evita
        assertThat(pen("10.0")).isEqualTo(pen("10.00"));
        assertThat(pen("10.0")).hasSameHashCodeAs(pen("10.00"));
    }

    @Test
    void isNotEqualWithDifferentCurrency() {
        assertThat(Money.of(new BigDecimal("10"), USD)).isNotEqualTo(pen("10"));
    }

    @Test
    void addsAmountsWithSameCurrency() {
        assertThat(pen("10.50").add(pen("0.25"))).isEqualTo(pen("10.75"));
    }

    @Test
    void rejectsAddingDifferentCurrency() {
        Money dollars = Money.of(new BigDecimal("1"), USD);

        assertThatThrownBy(() -> pen("10").add(dollars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void multipliesByQuantity() {
        assertThat(pen("19.90").multiply(3)).isEqualTo(pen("59.70"));
        assertThat(pen("19.90").multiply(0)).isEqualTo(Money.zero(PEN));
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> pen("10").multiply(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparesAmounts() {
        assertThat(pen("10.01").isGreaterThan(pen("10"))).isTrue();
        assertThat(pen("10").isGreaterThan(pen("10.00"))).isFalse();
    }

    @Test
    void detectsZero() {
        assertThat(Money.zero(PEN).isZero()).isTrue();
        assertThat(pen("0.01").isZero()).isFalse();
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> pen("-0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void rejectsNullValues() {
        assertThatThrownBy(() -> Money.of(null, PEN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }

}
