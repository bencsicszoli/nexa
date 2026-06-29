package com.nexa.support;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Teszt-segéd az előfizetés-gatinghez (#15): a feature-flow tesztek felhasználói az élessel
 * megegyezően aktív előfizetést igényelnek a premium végpontokhoz. A dev-szimulátor-végpontot
 * használja (a teszt-profilban {@code dev-controls=true}). A {@code authHeader} a teljes
 * {@code "Bearer <token>"} érték.
 */
public final class TestSubscriptions {

    private TestSubscriptions() {
    }

    /** Aktív előfizetést ad a tokenhez tartozó felhasználónak (hozzáférés a gated végpontokhoz). */
    public static void grantActive(MockMvc mockMvc, String authHeader) throws Exception {
        mockMvc.perform(post("/api/dev/subscription")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"));
    }
}
