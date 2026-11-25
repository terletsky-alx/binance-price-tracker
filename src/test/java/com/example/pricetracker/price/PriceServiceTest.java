package com.example.pricetracker.price;

import com.example.pricetracker.binance.BinanceProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceServiceTest {

    private static final String SYMBOL = "BNBUSDT";
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-30T00:00:00Z");

    private final PriceRecordRepository repository = mock(PriceRecordRepository.class);
    private final PriceService service = new PriceService(repository, new TrendCalculator(new BigDecimal("0.5")),
            new BinanceProperties("http://localhost", SYMBOL, Duration.ofSeconds(30),
                    Duration.ofSeconds(5), Duration.ofSeconds(10)));

    @Test
    void rejectsRangeWhereFromIsAfterTo() {
        assertThatThrownBy(() -> service.getAveragePrice(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundsAverageToPriceScale() {
        when(repository.findAveragePrice(SYMBOL, FROM, TO))
                .thenReturn(Optional.of(new BigDecimal("612.123456789012345")));

        assertThat(service.getAveragePrice(FROM, TO)).isEqualByComparingTo("612.12345679");
    }

    @Test
    void averageThrowsNotFoundWhenNoData() {
        when(repository.findAveragePrice(SYMBOL, FROM, TO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAveragePrice(FROM, TO)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void trendComparesFirstAndLastRecordOfPeriod() {
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(SYMBOL, FROM, TO))
                .thenReturn(Optional.of(record("600")));
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(SYMBOL, FROM, TO))
                .thenReturn(Optional.of(record("650")));

        assertThat(service.getTrend(FROM, TO)).isEqualTo(Trend.UPWARD);
    }

    private static PriceRecord record(String price) {
        return new PriceRecord(SYMBOL, new BigDecimal(price), FROM, FROM);
    }
}
