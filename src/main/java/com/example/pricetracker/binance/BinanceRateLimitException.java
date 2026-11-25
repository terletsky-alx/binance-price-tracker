package com.example.pricetracker.binance;

import java.time.Duration;

/**
 * Binance answered 429 (rate limit exceeded) or 418 (IP banned for ignoring 429).
 * Requests must be paused for {@code retryAfter}, otherwise the ban gets longer.
 */
public class BinanceRateLimitException extends RuntimeException {

    private final Duration retryAfter;

    public BinanceRateLimitException(int status, Duration retryAfter) {
        super("Binance rate limit hit, status " + status + ", retry after " + retryAfter);
        this.retryAfter = retryAfter;
    }

    /** May be null if Binance did not send Retry-After. */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
