package com.example.pricetracker.price;

import com.example.pricetracker.binance.AvgPriceResponse;
import com.example.pricetracker.binance.BinanceClient;
import com.example.pricetracker.binance.BinanceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Fetches the current average price from Binance and stores it. */
@Service
public class PriceCollector {

    private static final Logger log = LoggerFactory.getLogger(PriceCollector.class);

    private final BinanceClient binanceClient;
    private final PriceRecordRepository repository;
    private final BinanceProperties properties;
    private final Clock clock;

    public PriceCollector(BinanceClient binanceClient, PriceRecordRepository repository,
                          BinanceProperties properties, Clock clock) {
        this.binanceClient = binanceClient;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public void collect() {
        String symbol = properties.symbol();
        AvgPriceResponse response = binanceClient.getAveragePrice(symbol);

        // API contract requires fetchedAt as YYYY-MM-DDTHH:mm:ssZ, so seconds precision is enough
        Instant fetchedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        PriceRecord record = new PriceRecord(symbol, response.price(),
                Instant.ofEpochMilli(response.closeTime()), fetchedAt);

        if (repository.insertIfAbsent(record) == 1) {
            log.info("Saved {} price {} (closeTime {})", symbol, record.getPrice(), record.getCloseTime());
        } else {
            log.info("Skipped {} price: record with closeTime {} already exists", symbol, record.getCloseTime());
        }
    }
}
