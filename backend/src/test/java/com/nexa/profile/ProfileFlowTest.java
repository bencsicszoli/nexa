package com.nexa.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String register(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"Bob","password":"supersecret"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void profileEditAndAvatarUploadHappyPath() throws Exception {
        String token = register("bob@example.com");
        String auth = "Bearer " + token;

        // 1) Saját profil: kezdetben nincs bio/avatar.
        mockMvc.perform(get("/api/profile").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Bob"))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());

        // 2) Szerkesztés: név + bio.
        mockMvc.perform(patch("/api/profile").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Bob the Builder","bio":"Szeretek építeni."}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Bob the Builder"))
                .andExpect(jsonPath("$.bio").value("Szeretek építeni."));

        // 3) Avatar feltöltési link kérése.
        var uploadUrlResult = mockMvc.perform(post("/api/profile/avatar/upload-url")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/png"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.key").isNotEmpty())
                .andReturn();
        JsonNode upload = objectMapper.readTree(uploadUrlResult.getResponse().getContentAsString());
        String uploadUrl = upload.get("uploadUrl").asText();
        String key = upload.get("key").asText();
        assertThat(key).startsWith("avatars/");
        String uploadToken = uploadUrl.substring(uploadUrl.indexOf("token=") + "token=".length());

        // 4) A bájtok feltöltése az aláírt linkre.
        byte[] fakePng = "PNG\r\n\nfake-bytes".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(put("/api/storage/upload")
                        .param("token", uploadToken)
                        .contentType(MediaType.IMAGE_PNG)
                        .content(fakePng))
                .andExpect(status().isNoContent());

        // 5) Megerősítés → az avatarUrl a tárolt objektumra mutat.
        mockMvc.perform(put("/api/profile/avatar").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("key", key))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/api/media/" + key));

        // 6) A feltöltött kép publikusan kiszolgálható, a tartalom egyezik.
        mockMvc.perform(get("/api/media/" + key))
                .andExpect(status().isOk())
                .andExpect(content().bytes(fakePng));

        // 7) Avatar eltávolítása.
        mockMvc.perform(delete("/api/profile/avatar").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    void rejectsUnauthenticatedAndInvalidInput() throws Exception {
        // Token nélkül a profil védett.
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());

        String auth = "Bearer " + register("carol@example.com");

        // Üres név → validációs hiba.
        mockMvc.perform(patch("/api/profile").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"","bio":null}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Nem kép típus → elutasítás.
        mockMvc.perform(post("/api/profile/avatar/upload-url").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"application/pdf"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));

        // Hamis/idegen kulcs megerősítése → elutasítás.
        mockMvc.perform(put("/api/profile/avatar").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"secrets/passwords.txt"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
    }
}
