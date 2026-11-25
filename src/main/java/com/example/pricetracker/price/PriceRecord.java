package com.example.pricetracker.price;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_record")
public class PriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private BigDecimal price;

    /** Binance closeTime: time of the last trade included in the average. Used to detect duplicates. */
    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    /** Moment when the service received the price from Binance. */
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected PriceRecord() {
        // for JPA
    }

    public PriceRecord(String symbol, BigDecimal price, Instant closeTime, Instant fetchedAt) {
        this.symbol = symbol;
        this.price = price;
        this.closeTime = closeTime;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getCloseTime() {
        return closeTime;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
