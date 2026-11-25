package com.example.pricetracker.price;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TrendCalculatorTest {

    private final TrendCalculator calculator = new TrendCalculator(new BigDecimal("0.5"));

    @Test
    void upwardWhenPriceGrewMoreThanThreshold() {
        assertThat(calculator.calculate(new BigDecimal("100"), new BigDecimal("101"))).isEqualTo(Trend.UPWARD);
    }

    @Test
    void downwardWhenPriceDroppedMoreThanThreshold() {
        assertThat(calculator.calculate(new BigDecimal("100"), new BigDecimal("99"))).isEqualTo(Trend.DOWNWARD);
    }

    @Test
    void flatWhenChangeIsWithinThreshold() {
        assertThat(calculator.calculate(new BigDecimal("100"), new BigDecimal("100.3"))).isEqualTo(Trend.FLAT);
        assertThat(calculator.calculate(new BigDecimal("100"), new BigDecimal("99.7"))).isEqualTo(Trend.FLAT);
    }

    @Test
    void flatWhenChangeIsExactlyThreshold() {
        assertThat(calculator.calculate(new BigDecimal("100"), new BigDecimal("100.5"))).isEqualTo(Trend.FLAT);
    }

    @Test
    void flatWhenPriceDidNotChange() {
        assertThat(calculator.calculate(new BigDecimal("612.12345678"), new BigDecimal("612.12345678")))
                .isEqualTo(Trend.FLAT);
    }
}
