package com.example.pricetracker.web;

import com.example.pricetracker.price.PriceService;
import com.example.pricetracker.price.Trend;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Dates in from/to are ISO-8601 instants, e.g. 2026-09-01T00:00:00Z. Both bounds are inclusive. */
@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/latest")
    public PriceResponse getLatest() {
        return PriceResponse.from(priceService.getLatest());
    }

    @GetMapping("/average")
    public AveragePriceResponse getAverage(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
                                           @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant to) {
        return new AveragePriceResponse(priceService.getAveragePrice(from, to));
    }

    @GetMapping
    public PageResponse<PriceResponse> getPrices(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
                                                 @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(priceService.getPrices(from, to, page, size), PriceResponse::from);
    }

    @GetMapping("/trend")
    public TrendResponse getTrend(@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
                                  @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant to) {
        Trend trend = priceService.getTrend(from, to);
        return new TrendResponse(priceService.getSymbol(), from, to, trend);
    }
}
