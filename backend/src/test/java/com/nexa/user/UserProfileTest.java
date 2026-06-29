package com.nexa.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.support.TestSubscriptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A nyilvános profil végpont ({@code GET /api/users/{id}}) integrációs tesztje: a hívóhoz
 * viszonyított kapcsolatállapotot (ismerős + követés) helyesen tükrözi.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String[] register(String email, String name) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"%s","password":"supersecret"}"""
                                .formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        TestSubscriptions.grantActive(mockMvc, "Bearer " + token);
        return new String[]{token, body.get("user").get("id").asText()};
    }

    @Test
    void publicProfileReflectsRelationshipState() throws Exception {
        String[] a = register("ada@users.com", "Ada");
        String[] b = register("ben@users.com", "Ben");
        String authA = "Bearer " + a[0];

        // Idegen profil: nincs kapcsolat, nem követi, nem saját, e-mailt nem szivárogtatunk.
        mockMvc.perform(get("/api/users/" + b[1]).header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ben"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.self").value(false))
                .andExpect(jsonPath("$.friendStatus").value("NONE"))
                .andExpect(jsonPath("$.following").value(false));

        // Saját profil → self.
        mockMvc.perform(get("/api/users/" + a[1]).header("Authorization", authA))
                .andExpect(jsonPath("$.self").value(true))
                .andExpect(jsonPath("$.friendStatus").value("SELF"));

        // A követi B-t → following igaz.
        mockMvc.perform(put("/api/follows/" + b[1]).header("Authorization", authA))
                .andExpect(status().isNoContent());

        // A ismerőskérést küld B-nek → REQUEST_SENT, kitöltött friendRequestId.
        mockMvc.perform(post("/api/friends/requests").header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(b[1])))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/" + b[1]).header("Authorization", authA))
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.friendStatus").value("REQUEST_SENT"))
                .andExpect(jsonPath("$.friendRequestId").isNotEmpty());
    }

    @Test
    void unknownUserIs404AndEndpointRequiresAuth() throws Exception {
        String[] a = register("cleo@users.com", "Cleo");

        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + a[0]))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        mockMvc.perform(get("/api/users/" + a[1]))
                .andExpect(status().isUnauthorized());
    }
}
