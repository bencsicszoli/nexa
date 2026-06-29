package com.nexa.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Jelszóváltás (#17): a jelenlegi jelszó ellenőrzése (jó/rossz), és hogy siker után a korábbi
 * refresh tokenek visszavonódnak (a régi refresh token már nem váltható be).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChangePasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void wrongCurrentRejected_thenSuccessRevokesRefreshTokens() throws Exception {
        var reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pw@example.com","displayName":"Pim","password":"supersecret"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(reg.getResponse().getContentAsString());
        String auth = "Bearer " + body.get("accessToken").asText();
        String oldRefresh = body.get("refreshToken").asText();

        // Rossz jelenlegi jelszó → 400 WRONG_PASSWORD.
        mockMvc.perform(post("/api/settings/password").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"nope12345\",\"newPassword\":\"brandnew123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));

        // Jó jelenlegi jelszó → 204.
        mockMvc.perform(post("/api/settings/password").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"supersecret\",\"newPassword\":\"brandnew123\"}"))
                .andExpect(status().isNoContent());

        // A régi refresh token visszavonva → 401.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(oldRefresh)))
                .andExpect(status().isUnauthorized());

        // Az új jelszóval be lehet lépni.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pw@example.com\",\"password\":\"brandnew123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
