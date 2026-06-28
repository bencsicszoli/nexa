package com.nexa.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexa.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Kimenő hívások a Paddle REST API felé (szerver-oldali API kulccsal).
 * Jelenleg a számlázási portál-session létrehozását fedi le; a kártyaadat
 * sosem érinti a szervert — a portál a Paddle hosztolt felülete.
 */
@Component
public class PaddleClient {

    private static final Logger log = LoggerFactory.getLogger(PaddleClient.class);

    private final PaddleProperties properties;
    private final RestClient restClient;

    public PaddleClient(PaddleProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }

    /**
     * Számlázási portál-session a megadott Paddle customerhez. A válaszból a
     * {@code data.urls.general.overview} URL-t adjuk vissza — ezen a vásárló
     * kezelheti az előfizetését, kártyáját, lemondását.
     */
    public String createPortalSession(String customerId) {
        if (properties.getApiKey().isBlank()) {
            throw ApiException.subscriptionNotConfigured();
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/customers/{id}/portal-sessions", customerId)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(JsonNode.class);

            String url = response != null
                    ? response.path("data").path("urls").path("general").path("overview").asText(null)
                    : null;
            if (url == null || url.isBlank()) {
                log.warn("A Paddle portal-session válasz nem tartalmazott URL-t: {}", response);
                throw ApiException.subscriptionNotConfigured();
            }
            return url;
        } catch (RestClientException e) {
            log.warn("Paddle portal-session hívás sikertelen (customer {}): {}", customerId, e.getMessage());
            throw ApiException.subscriptionNotConfigured();
        }
    }
}
