package com.example.pricetracker.web;

import com.example.pricetracker.price.PriceRecord;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

/** Price is written as a JSON string, so clients don't lose precision by parsing it into a double. */
public record PriceResponse(
        String symbol,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal price,
        Instant fetchedAt
) {
    public static PriceResponse from(PriceRecord record) {
        return new PriceResponse(record.getSymbol(), record.getPrice(), record.getFetchedAt());
    }
}
