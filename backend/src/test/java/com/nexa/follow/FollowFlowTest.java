package com.nexa.follow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.support.TestSubscriptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FollowFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Regisztrál egy felhasználót, és visszaadja az [accessToken, userId] párt. */
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
    void followAppearsInBothListsAndUnfollowRemovesIt() throws Exception {
        String[] a = register("liam@example.com", "Liam");
        String[] b = register("mia@example.com", "Mia");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];
        String idB = b[1];

        // Kezdetben üres minden lista.
        mockMvc.perform(get("/api/follows/following").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // A követi B-t.
        mockMvc.perform(put("/api/follows/" + idB).header("Authorization", authA))
                .andExpect(status().isNoContent());

        // A követési listáján megjelenik Mia...
        mockMvc.perform(get("/api/follows/following").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Mia"))
                .andExpect(jsonPath("$[0].since").isNotEmpty());

        // ...B követői listáján pedig Liam.
        mockMvc.perform(get("/api/follows/followers").header("Authorization", authB))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Liam"));

        // Lekövetés → mindkét listából eltűnik.
        mockMvc.perform(delete("/api/follows/" + idB).header("Authorization", authA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/follows/following").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/follows/followers").header("Authorization", authB))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void followIsIdempotentAndUnfollowOnMissingIsNoop() throws Exception {
        String[] a = register("noah@example.com", "Noah");
        String[] b = register("olivia@example.com", "Olivia");
        String authA = "Bearer " + a[0];
        String idB = b[1];

        // Lekövetés követés nélkül → nincs hiba.
        mockMvc.perform(delete("/api/follows/" + idB).header("Authorization", authA))
                .andExpect(status().isNoContent());

        // Kétszeri követés → idempotens, egyetlen rekord.
        mockMvc.perform(put("/api/follows/" + idB).header("Authorization", authA))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/follows/" + idB).header("Authorization", authA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/follows/following").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cannotFollowSelfAndUnknownUserIs404() throws Exception {
        String[] a = register("peter@example.com", "Peter");
        String authA = "Bearer " + a[0];

        // Saját maga követése → 400.
        mockMvc.perform(put("/api/follows/" + a[1]).header("Authorization", authA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_FOLLOW_SELF"));

        // Nem létező felhasználó követése → 404.
        mockMvc.perform(put("/api/follows/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", authA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        // Token nélkül védett.
        mockMvc.perform(get("/api/follows/following"))
                .andExpect(status().isUnauthorized());
    }
}
