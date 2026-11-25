package com.example.pricetracker;

import com.example.pricetracker.price.PricePollingJob;
import com.example.pricetracker.price.PriceRecordRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real PostgreSQL in Docker (Testcontainers) + fake Binance (WireMock).
 * Scheduling is disabled, polling is triggered by hand to keep the test deterministic.
 */
@SpringBootTest(properties = "scheduling.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class PriceTrackerIntegrationTest {

    private static final String FROM = "2000-01-01T00:00:00Z";
    private static final String TO = "2100-01-01T00:00:00Z";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static WireMockServer binance = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        binance.start();
    }

    @DynamicPropertySource
    static void binanceProperties(DynamicPropertyRegistry registry) {
        registry.add("binance.base-url", binance::baseUrl);
        registry.add("binance.symbol", () -> "BNBUSDT");
    }

    @AfterAll
    static void stopWireMock() {
        binance.stop();
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PricePollingJob pollingJob;

    @Autowired
    PriceRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
        binance.resetAll();
    }

    @Test
    void savesPricesWithoutDuplicatesAndServesThemOverRest() throws Exception {
        stubAvgPrice("600.00000000", 1_700_000_000_000L);
        pollingJob.poll();
        pollingJob.poll(); // same closeTime -> must not create a duplicate
        stubAvgPrice("660.00000000", 1_700_000_060_000L);
        pollingJob.poll();

        assertThat(repository.count()).isEqualTo(2);

        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.price").value("660.00000000"))
                .andExpect(jsonPath("$.fetchedAt").value(matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")));

        mockMvc.perform(get("/api/v1/prices/average").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averagePrice").value(630.0));

        mockMvc.perform(get("/api/v1/prices").param("from", FROM).param("to", TO)
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].price").value("600.00000000"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/prices/trend").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.start_date").value(FROM))
                .andExpect(jsonPath("$.end_date").value(TO))
                .andExpect(jsonPath("$.trend").value("UPWARD"));
    }

    @Test
    void binanceErrorDoesNotSaveAnythingAndDoesNotBreakPolling() throws Exception {
        binance.stubFor(WireMock.get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(aResponse().withStatus(500)));

        pollingJob.poll();

        assertThat(repository.count()).isZero();
        mockMvc.perform(get("/api/v1/prices/latest")).andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForInvalidRange() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("from", TO).param("to", FROM))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/prices/average").param("from", "yesterday").param("to", TO))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/prices").param("from", FROM).param("to", TO).param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    private static void stubAvgPrice(String price, long closeTime) {
        binance.stubFor(WireMock.get(urlPathEqualTo("/api/v3/avgPrice"))
                .willReturn(okJson("""
                        {"mins": 5, "price": "%s", "closeTime": %d}
                        """.formatted(price, closeTime))));
    }
}
