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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Az előfizetés-gating (#15) H2-n: a paywall-mögötti (premium) végpontok {@code 402
 * SUBSCRIPTION_REQUIRED}-et adnak hozzáférés nélkül, és elérhetővé válnak, amint a dev-szimulátor
 * ({@code /api/dev/subscription}, csak {@code dev-controls=true} esetén) hozzáférő állapotot állít be.
 * A nem gate-elt végpontok (billing-állapot, saját profil) mindig elérhetők.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionGatingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void gatingFollowsEntitlementState() throws Exception {
        String token = register("gating@example.com");

        // 1. Friss user → NONE → a hírfolyam és a posztolás paywall mögött (402)
        mockMvc.perform(get("/api/subscriptions/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.hasAccess").value(false));
        feed(token).andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_REQUIRED"));
        createPost(token).andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_REQUIRED"));

        // De a nem gate-elt végpontok NONE alatt is mennek (a usernek el kell érnie a billinget):
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        // 2. ACTIVE → hozzáfér: feed 200, poszt 201
        setState(token, "ACTIVE", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAccess").value(true));
        feed(token).andExpect(status().isOk());
        createPost(token).andExpect(status().isCreated());

        // 3. Folyamatban lévő trial → hozzáfér
        setState(token, "TRIALING", 7).andExpect(jsonPath("$.hasAccess").value(true));
        feed(token).andExpect(status().isOk());

        // 4. Lejárt trial → nincs hozzáférés (402)
        setState(token, "TRIALING", -1).andExpect(jsonPath("$.hasAccess").value(false));
        feed(token).andExpect(status().isPaymentRequired());

        // 5. PAST_DUE → grace, hozzáfér
        setState(token, "PAST_DUE", null).andExpect(jsonPath("$.hasAccess").value(true));
        feed(token).andExpect(status().isOk());

        // 6. PAUSED és CANCELED → nincs hozzáférés
        setState(token, "PAUSED", null).andExpect(jsonPath("$.hasAccess").value(false));
        feed(token).andExpect(status().isPaymentRequired());
        setState(token, "CANCELED", null).andExpect(jsonPath("$.hasAccess").value(false));
        feed(token).andExpect(status().isPaymentRequired());
    }

    // --- segédek ---

    private String register(String email) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"Gating Tester","password":"supersecret"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private org.springframework.test.web.servlet.ResultActions feed(String token) throws Exception {
        return mockMvc.perform(get("/api/feed").header("Authorization", bearer(token)));
    }

    private org.springframework.test.web.servlet.ResultActions createPost(String token) throws Exception {
        return mockMvc.perform(post("/api/posts")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content":"Helló, ez egy teszt poszt."}"""));
    }

    private org.springframework.test.web.servlet.ResultActions setState(
            String token, String status, Integer trialDaysFromNow) throws Exception {
        JsonNode body = trialDaysFromNow == null
                ? objectMapper.createObjectNode().put("status", status)
                : objectMapper.createObjectNode().put("status", status).put("trialDaysFromNow", trialDaysFromNow);
        return mockMvc.perform(post("/api/dev/subscription")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
