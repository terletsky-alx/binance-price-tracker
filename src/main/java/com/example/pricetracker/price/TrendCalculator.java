package com.example.pricetracker.price;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Compares the first and the last price of the period.
 * If the change is within ±flatThresholdPercent the trend is FLAT, otherwise UPWARD or DOWNWARD.
 */
@Component
public class TrendCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 8;

    private final BigDecimal flatThresholdPercent;

    public TrendCalculator(@Value("${trend.flat-threshold-percent}") BigDecimal flatThresholdPercent) {
        this.flatThresholdPercent = flatThresholdPercent;
    }

    public Trend calculate(BigDecimal firstPrice, BigDecimal lastPrice) {
        BigDecimal changePercent = lastPrice.subtract(firstPrice)
                .multiply(HUNDRED)
                .divide(firstPrice, PERCENT_SCALE, RoundingMode.HALF_UP);

        if (changePercent.abs().compareTo(flatThresholdPercent) <= 0) {
            return Trend.FLAT;
        }
        return changePercent.signum() > 0 ? Trend.UPWARD : Trend.DOWNWARD;
    }
}
