package com.example.pricetracker.price;

import com.example.pricetracker.binance.BinanceRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class PricePollingJob {

    private static final Logger log = LoggerFactory.getLogger(PricePollingJob.class);

    private final PriceCollector collector;
    private final Clock clock;

    /** Set after 429/418 from Binance. Only touched from the single scheduler thread. */
    private Instant pausedUntil = Instant.MIN;

    public PricePollingJob(PriceCollector collector, Clock clock) {
        this.collector = collector;
        this.clock = clock;
    }

    /**
     * fixedDelay (not fixedRate): the next run starts only after the previous one finished,
     * so slow Binance responses never cause overlapping runs.
     * Every exception is caught and logged, so one failed call never affects the next runs.
     */
    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void poll() {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            log.warn("Polling paused until {} because of Binance rate limit", pausedUntil);
            return;
        }

        try {
            collector.collect();
        } catch (BinanceRateLimitException e) {
            if (e.getRetryAfter() != null) {
                pausedUntil = now.plus(e.getRetryAfter());
            }
            log.warn(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch price from Binance, will retry on next run", e);
        }
    }
}
