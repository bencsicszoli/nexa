package com.nexa.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Az előfizetés webhook-életciklusa H2-n: regisztráció → NONE, majd helyesen
 * aláírt Paddle webhookok hatására TRIALING → ACTIVE → CANCELED. A hibás
 * aláírású webhookot elutasítjuk (nincs állapotváltozás).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionWebhookTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaddleWebhookVerifier verifier;

    @Test
    void webhookLifecycleUpdatesSubscriptionState() throws Exception {
        // 1. Regisztráció → access token + userId
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sub@example.com","displayName":"Sub Tester","password":"supersecret"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(registered.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        String userId = body.get("user").get("id").asText();

        // 2. Kezdetben nincs előfizetés → NONE
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"));

        // 3. subscription.created (trialing) → TRIALING + trialEndsAt
        postWebhook(subscriptionEvent("subscription.created", "trialing", userId,
                "pri_monthly_test", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIALING"))
                .andExpect(jsonPath("$.plan").value("MONTHLY"))
                .andExpect(jsonPath("$.trialEndsAt").isNotEmpty());

        // 4. subscription.activated (active) → ACTIVE + renewsAt
        postWebhook(subscriptionEvent("subscription.activated", "active", userId,
                "pri_monthly_test", "2030-02-01T00:00:00Z"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.renewsAt").isNotEmpty());

        // 5. Hibás aláírás → 401, nincs állapotváltozás
        String tamperedBody = subscriptionEvent("subscription.canceled", "canceled", userId,
                "pri_monthly_test", "2030-02-01T00:00:00Z");
        String ts = String.valueOf(Instant.now().getEpochSecond());
        mockMvc.perform(post("/api/webhooks/paddle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Paddle-Signature", "ts=" + ts + ";h1=deadbeef")
                        .content(tamperedBody))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 6. subscription.canceled (helyes aláírással) → CANCELED
        postWebhook(subscriptionEvent("subscription.canceled", "canceled", userId,
                "pri_monthly_test", "2030-02-01T00:00:00Z"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledAt").isNotEmpty());
    }

    /** Helyesen aláírt webhook POST (a verifier sign() metódusával). */
    private org.springframework.test.web.servlet.ResultActions postWebhook(String json) throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String signature = "ts=" + ts + ";h1=" + verifier.sign(ts, json);
        return mockMvc.perform(post("/api/webhooks/paddle")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Paddle-Signature", signature)
                .content(json));
    }

    private static String subscriptionEvent(String eventType, String paddleStatus, String userId,
                                            String priceId, String nextBilledAt) {
        return """
                {
                  "event_type": "%s",
                  "data": {
                    "id": "sub_test_1",
                    "status": "%s",
                    "customer_id": "ctm_test_1",
                    "next_billed_at": "%s",
                    "trial_dates": { "ends_at": "%s" },
                    "items": [ { "price": { "id": "%s" } } ],
                    "custom_data": { "userId": "%s" }
                  }
                }""".formatted(eventType, paddleStatus, nextBilledAt, nextBilledAt, priceId, userId);
    }
}
