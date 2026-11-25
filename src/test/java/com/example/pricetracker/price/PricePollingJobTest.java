package com.example.pricetracker.price;

import com.example.pricetracker.binance.BinanceRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PricePollingJobTest {

    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");

    private final PriceCollector collector = mock(PriceCollector.class);

    @Test
    void doesNotThrowWhenBinanceCallFails() {
        PricePollingJob job = new PricePollingJob(collector, Clock.fixed(NOW, ZoneOffset.UTC));
        doThrow(new RuntimeException("connection refused")).when(collector).collect();

        assertThatCode(job::poll).doesNotThrowAnyException();
        assertThatCode(job::poll).doesNotThrowAnyException();

        verify(collector, times(2)).collect();
    }

    @Test
    void pausesPollingForRetryAfterOnRateLimit() {
        MutableClock clock = new MutableClock(NOW);
        PricePollingJob job = new PricePollingJob(collector, clock);
        doThrow(new BinanceRateLimitException(429, Duration.ofSeconds(60))).when(collector).collect();

        job.poll();
        reset(collector);

        clock.now = NOW.plusSeconds(30);
        job.poll();
        verify(collector, never()).collect();

        clock.now = NOW.plusSeconds(60);
        job.poll();
        verify(collector).collect();
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }
}
