package com.example.pricetracker.price;

import com.example.pricetracker.binance.BinanceProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class PriceService {

    /** Same scale as the price column. */
    private static final int PRICE_SCALE = 8;

    private final PriceRecordRepository repository;
    private final TrendCalculator trendCalculator;
    private final String symbol;

    public PriceService(PriceRecordRepository repository, TrendCalculator trendCalculator,
                        BinanceProperties properties) {
        this.repository = repository;
        this.trendCalculator = trendCalculator;
        this.symbol = properties.symbol();
    }

    public String getSymbol() {
        return symbol;
    }

    public PriceRecord getLatest() {
        return repository.findFirstBySymbolOrderByFetchedAtDesc(symbol)
                .orElseThrow(() -> new NotFoundException("No prices saved yet"));
    }

    public BigDecimal getAveragePrice(Instant from, Instant to) {
        validateRange(from, to);
        return repository.findAveragePrice(symbol, from, to)
                .map(avg -> avg.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                .orElseThrow(() -> new NotFoundException("No prices in the given period"));
    }

    public Page<PriceRecord> getPrices(Instant from, Instant to, int page, int size) {
        validateRange(from, to);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("fetchedAt"));
        return repository.findBySymbolAndFetchedAtBetween(symbol, from, to, pageRequest);
    }

    public Trend getTrend(Instant from, Instant to) {
        validateRange(from, to);
        PriceRecord first = repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, from, to)
                .orElseThrow(() -> new NotFoundException("No prices in the given period"));
        PriceRecord last = repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, from, to)
                .orElseThrow(() -> new NotFoundException("No prices in the given period"));
        return trendCalculator.calculate(first.getPrice(), last.getPrice());
    }

    private static void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }
}
