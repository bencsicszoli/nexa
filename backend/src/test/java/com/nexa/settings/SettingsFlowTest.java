package com.nexa.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A beállítások (#17) végpontjainak flow-tesztje: alapértékek → nyelv mentése (a /auth/me is
 * tükrözi) → értesítési preferenciák és adatvédelmi kapcsolók körbejárása. Ez korán megfogja a
 * JSON-tárolás (NotificationPrefs) H2-kompatibilitási gondjait is. A végpontok nem gateltek.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettingsFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String register(String email, String name) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"%s","password":"supersecret"}"""
                                .formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + body.get("accessToken").asText();
    }

    @Test
    void defaultsThenUpdateLocaleReflectedInMe() throws Exception {
        String auth = register("settings-a@example.com", "Anna");

        // Alapértékek.
        mockMvc.perform(get("/api/settings").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("hu"))
                .andExpect(jsonPath("$.searchable").value(true))
                .andExpect(jsonPath("$.hidePresence").value(false))
                .andExpect(jsonPath("$.totpEnabled").value(false))
                .andExpect(jsonPath("$.notificationPrefs.newPost").value(true))
                .andExpect(jsonPath("$.notificationPrefs.friendRequest").value(true))
                .andExpect(jsonPath("$.notificationPrefs.friendAccepted").value(true))
                .andExpect(jsonPath("$.notificationPrefs.newFollower").value(true));

        // Nyelv mentése.
        mockMvc.perform(patch("/api/settings/locale").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locale\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("en"));

        // A /auth/me is tükrözi.
        mockMvc.perform(get("/api/auth/me").header("Authorization", auth))
                .andExpect(jsonPath("$.locale").value("en"));
    }

    @Test
    void notificationsAndPrivacyRoundTrip() throws Exception {
        String auth = register("settings-b@example.com", "Bence");

        mockMvc.perform(patch("/api/settings/notifications").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPost":false,"friendRequest":true,"friendAccepted":false,"newFollower":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationPrefs.newPost").value(false))
                .andExpect(jsonPath("$.notificationPrefs.friendAccepted").value(false));

        mockMvc.perform(patch("/api/settings/privacy").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchable\":false,\"hidePresence\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchable").value(false))
                .andExpect(jsonPath("$.hidePresence").value(true));

        // Újraolvasva is megmarad (JSON-tárolás + flag-ek perzisztáltak).
        mockMvc.perform(get("/api/settings").header("Authorization", auth))
                .andExpect(jsonPath("$.notificationPrefs.newPost").value(false))
                .andExpect(jsonPath("$.notificationPrefs.friendRequest").value(true))
                .andExpect(jsonPath("$.searchable").value(false))
                .andExpect(jsonPath("$.hidePresence").value(true));
    }

    @Test
    void invalidLocaleRejected() throws Exception {
        String auth = register("settings-c@example.com", "Cili");
        mockMvc.perform(patch("/api/settings/locale").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locale\":\"de\"}"))
                .andExpect(status().isBadRequest());
    }
}
