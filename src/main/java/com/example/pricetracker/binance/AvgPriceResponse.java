package com.example.pricetracker.binance;

import java.math.BigDecimal;

/**
 * Response of GET /api/v3/avgPrice.
 *
 * @param mins      averaging window in minutes
 * @param price     average price (Binance sends it as a string, Jackson parses it into BigDecimal without loss)
 * @param closeTime time of the last trade in the window, epoch millis
 */
public record AvgPriceResponse(int mins, BigDecimal price, long closeTime) {
}
