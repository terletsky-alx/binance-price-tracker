package com.example.pricetracker.web;

import com.example.pricetracker.price.Trend;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TrendResponse(
        String symbol,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("end_date") Instant endDate,
        Trend trend
) {
}
