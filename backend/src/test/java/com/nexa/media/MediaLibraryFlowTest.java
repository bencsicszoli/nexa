package com.nexa.media;

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

/**
 * A személyes médiatár (Médiatár) végpontjainak integrációs tesztje: presigned feltöltés →
 * megerősítés → listázás → törlés (a fájl is törlődik), valamint a tulajdonos- és a
 * gating-szabályok. A {@code PostFlowTest} mintáját követi (H2, MockMvc, TestSubscriptions).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MediaLibraryFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Regisztrál egy felhasználót, és aktív előfizetést ad neki. */
    private String register(String email) throws Exception {
        String token = registerWithoutSubscription(email);
        TestSubscriptions.grantActive(mockMvc, "Bearer " + token);
        return token;
    }

    /** Regisztrál egy felhasználót előfizetés nélkül (a paywall/gating teszteléséhez). */
    private String registerWithoutSubscription(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"Mara","password":"supersecret"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void uploadConfirmAndListMedia() throws Exception {
        String auth = "Bearer " + register("lib-list@example.com");

        // 1) Aláírt feltöltési cél.
        var uploadResult = mockMvc.perform(post("/api/library/upload-url")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/png"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.key").isNotEmpty())
                .andReturn();
        JsonNode upload = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String uploadUrl = upload.get("uploadUrl").asText();
        String key = upload.get("key").asText();

        // 2) Bájtok feltöltése a presigned URL-re.
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        mockMvc.perform(put(uploadUrl)
                        .contentType(MediaType.IMAGE_PNG)
                        .content(fakePng))
                .andExpect(status().isNoContent());

        // 3) Megerősítés (perzisztálás a médiatárba).
        mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"%s","type":"IMAGE","sizeBytes":8}""".formatted(key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("IMAGE"))
                .andExpect(jsonPath("$.url").value("/api/media/" + key))
                .andExpect(jsonPath("$.sizeBytes").value(8));

        // 4) Listázás: tartalmazza az új elemet.
        mockMvc.perform(get("/api/library").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].url").value("/api/media/" + key));

        // 5) A feltöltött fájl kiszolgálható.
        mockMvc.perform(get("/api/media/" + key))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        String auth = "Bearer " + register("lib-badtype@example.com");

        mockMvc.perform(post("/api/library/upload-url")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"application/zip"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void rejectsConfirmWithKeyOutsideLibraryPrefix() throws Exception {
        String auth = "Bearer " + register("lib-prefix@example.com");

        mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"posts/x.png","type":"IMAGE","sizeBytes":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
    }

    @Test
    void deletingMediaItemAlsoDeletesItsFile() throws Exception {
        String auth = "Bearer " + register("lib-delete@example.com");

        var uploadResult = mockMvc.perform(post("/api/library/upload-url")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/png"}"""))
                .andReturn();
        JsonNode upload = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String uploadUrl = upload.get("uploadUrl").asText();
        String key = upload.get("key").asText();

        mockMvc.perform(put(uploadUrl).contentType(MediaType.IMAGE_PNG)
                        .content(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}))
                .andExpect(status().isNoContent());

        var confirmResult = mockMvc.perform(post("/api/library").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"%s","type":"IMAGE","sizeBytes":4}""".formatted(key)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(confirmResult.getResponse().getContentAsString())
                .get("id").asText();

        // A fájl megvan.
        mockMvc.perform(get("/api/media/" + key)).andExpect(status().isOk());

        // Törlés → a fájl is törlődik (commit után).
        mockMvc.perform(delete("/api/library/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/media/" + key)).andExpect(status().isNotFound());

        // A lista üres.
        mockMvc.perform(get("/api/library").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotDeleteOthersMediaItem() throws Exception {
        String ownerAuth = "Bearer " + register("lib-owner@example.com");
        String otherAuth = "Bearer " + register("lib-other@example.com");

        var uploadResult = mockMvc.perform(post("/api/library/upload-url")
                        .header("Authorization", ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/png"}"""))
                .andReturn();
        JsonNode upload = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String uploadUrl = upload.get("uploadUrl").asText();
        String key = upload.get("key").asText();

        mockMvc.perform(put(uploadUrl).contentType(MediaType.IMAGE_PNG)
                        .content(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})).andExpect(status().isNoContent());

        var confirmResult = mockMvc.perform(post("/api/library").header("Authorization", ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"%s","type":"IMAGE","sizeBytes":4}""".formatted(key)))
                .andReturn();
        String id = objectMapper.readTree(confirmResult.getResponse().getContentAsString())
                .get("id").asText();

        // Másik felhasználó nem törölheti — 404 (a létezést sem szivárogtatjuk).
        mockMvc.perform(delete("/api/library/" + id).header("Authorization", otherAuth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDIA_NOT_FOUND"));
    }

    @Test
    void requiresSubscription() throws Exception {
        String auth = "Bearer " + registerWithoutSubscription("lib-paywall@example.com");

        mockMvc.perform(get("/api/library").header("Authorization", auth))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_REQUIRED"));
    }
}
