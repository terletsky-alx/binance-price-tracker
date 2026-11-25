package com.example.pricetracker.binance;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class BinanceClient {

    private static final int TOO_MANY_REQUESTS = 429;
    private static final int IP_BANNED = 418;

    private final RestClient restClient;

    public BinanceClient(RestClient.Builder builder, BinanceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Calls GET /api/v3/avgPrice. Any non-2xx response is thrown as an exception:
     * 429/418 as {@link BinanceRateLimitException}, others as RestClientResponseException.
     */
    public AvgPriceResponse getAveragePrice(String symbol) {
        AvgPriceResponse response = restClient.get()
                .uri(uri -> uri.path("/api/v3/avgPrice").queryParam("symbol", symbol).build())
                .retrieve()
                .onStatus(BinanceClient::isRateLimited, (request, httpResponse) -> {
                    throw new BinanceRateLimitException(
                            httpResponse.getStatusCode().value(),
                            parseRetryAfter(httpResponse.getHeaders()));
                })
                .body(AvgPriceResponse.class);

        if (response == null || response.price() == null) {
            throw new IllegalStateException("Binance returned an empty avgPrice response");
        }
        return response;
    }

    private static boolean isRateLimited(HttpStatusCode status) {
        return status.value() == TOO_MANY_REQUESTS || status.value() == IP_BANNED;
    }

    private static Duration parseRetryAfter(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
