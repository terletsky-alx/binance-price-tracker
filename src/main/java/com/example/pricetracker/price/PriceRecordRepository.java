package com.example.pricetracker.price;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    /**
     * Inserts a record unless one with the same symbol and closeTime already exists.
     *
     * @return 1 if the row was inserted, 0 if it was a duplicate
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO price_record (symbol, price, close_time, fetched_at)
            VALUES (:#{#record.symbol}, :#{#record.price}, :#{#record.closeTime}, :#{#record.fetchedAt})
            ON CONFLICT (symbol, close_time) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(PriceRecord record);

    Optional<PriceRecord> findFirstBySymbolOrderByFetchedAtDesc(String symbol);

    Page<PriceRecord> findBySymbolAndFetchedAtBetween(String symbol, Instant from, Instant to, Pageable pageable);

    Optional<PriceRecord> findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(String symbol, Instant from, Instant to);

    Optional<PriceRecord> findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(String symbol, Instant from, Instant to);

    /** Native query: JPQL AVG returns Double, here PostgreSQL keeps NUMERIC precision. */
    @Query(value = """
            SELECT AVG(price) FROM price_record
            WHERE symbol = :symbol AND fetched_at BETWEEN :from AND :to
            """, nativeQuery = true)
    Optional<BigDecimal> findAveragePrice(String symbol, Instant from, Instant to);
}
