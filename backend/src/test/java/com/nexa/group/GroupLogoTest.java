package com.nexa.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.support.TestSubscriptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A csoport-logó feltöltés (presigned URL) és a logóval/anélkül létrehozott csoport
 * integrációs tesztje.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupLogoTest {

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
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        TestSubscriptions.grantActive(mockMvc, "Bearer " + token);
        return token;
    }

    @Test
    void createGroupWithUploadedLogoExposesLogoUrl() throws Exception {
        String auth = "Bearer " + register("luca@logo.com", "Luca");

        // Aláírt logó-feltöltési cél; a kulcs a group-logos mappába mutat.
        var urlResult = mockMvc.perform(post("/api/groups/logo/upload-url").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.key").isNotEmpty())
                .andReturn();
        String key = objectMapper.readTree(urlResult.getResponse().getContentAsString())
                .get("key").asText();
        assertThat(key).startsWith("group-logos/");

        // A logóval létrehozott csoport DTO-jában a logoUrl ki van töltve.
        mockMvc.perform(post("/api/groups").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fotó kör\",\"description\":\"\",\"logoKey\":\"%s\"}".formatted(key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fotó kör"))
                .andExpect(jsonPath("$.logoUrl").isNotEmpty());
    }

    @Test
    void createGroupWithoutLogoHasNullLogoUrlAndBadKeyIsRejected() throws Exception {
        String auth = "Bearer " + register("mara@logo.com", "Mara");

        // Logó nélkül a logoUrl null (a frontend monogramot mutat).
        mockMvc.perform(post("/api/groups").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kódklub\",\"description\":\"\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logoUrl").value(nullValue()));

        // Idegen mappába mutató kulcs → elutasítva (nem lehet tetszőleges objektumot logónak állítani).
        mockMvc.perform(post("/api/groups").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hamis\",\"description\":\"\",\"logoKey\":\"avatars/x.png\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
    }
}
