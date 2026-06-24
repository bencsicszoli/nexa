package com.nexa.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String register(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"Dana","password":"supersecret"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void createAndListOwnPostsChronologically() throws Exception {
        String auth = "Bearer " + register("dana@example.com");

        // Kezdetben nincs bejegyzés.
        mockMvc.perform(get("/api/posts/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Első bejegyzés.
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Szia, Nexa! Ez az első posztom."}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.content").value("Szia, Nexa! Ez az első posztom."))
                .andExpect(jsonPath("$.authorName").value("Dana"))
                .andExpect(jsonPath("$.media.length()").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // Második bejegyzés.
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"A második bejegyzés."}"""))
                .andExpect(status().isCreated());

        // A lista legfrissebb felül (a második poszt elöl).
        mockMvc.perform(get("/api/posts/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("A második bejegyzés."))
                .andExpect(jsonPath("$[1].content").value("Szia, Nexa! Ez az első posztom."));
    }

    @Test
    void rejectsUnauthenticatedAndEmptyPost() throws Exception {
        // Token nélkül védett.
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"akármi"}"""))
                .andExpect(status().isUnauthorized());

        String auth = "Bearer " + register("erik@example.com");

        // Sem szöveg, sem média → EMPTY_POST.
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_POST"));
    }

    @Test
    void uploadMediaAndCreatePostWithImage() throws Exception {
        String auth = "Bearer " + register("mira@example.com");

        // 1) Aláírt feltöltési cél kérése képhez.
        var uploadResult = mockMvc.perform(post("/api/posts/media/upload-url")
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

        // 2) A bájtok feltöltése az aláírt URL-re (lokál tárolónál a backend nyeli el).
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        mockMvc.perform(put(uploadUrl)
                        .contentType(MediaType.IMAGE_PNG)
                        .content(fakePng))
                .andExpect(status().isNoContent());

        // 3) Poszt létrehozása a feltöltött média kulcsával (szöveg nélkül is mehet).
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"","media":[{"key":"%s","type":"IMAGE","sizeBytes":8}]}"""
                                .formatted(key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.media.length()").value(1))
                .andExpect(jsonPath("$.media[0].type").value("IMAGE"))
                .andExpect(jsonPath("$.media[0].url").value("/api/media/" + key))
                .andExpect(jsonPath("$.media[0].sizeBytes").value(8));

        // 4) A feltöltött kép kiszolgálható a publikus URL-ről.
        mockMvc.perform(get("/api/media/" + key))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnsupportedMediaType() throws Exception {
        String auth = "Bearer " + register("nora@example.com");

        mockMvc.perform(post("/api/posts/media/upload-url")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"application/zip"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void rejectsMediaKeyOutsidePostsPrefix() throws Exception {
        String auth = "Bearer " + register("pepe@example.com");

        // Más mappára (pl. avatars/) mutató kulcs nem fogadható el poszt-médiaként.
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hopp","media":[{"key":"avatars/x.png","type":"IMAGE","sizeBytes":1}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
    }

    @Test
    void acceptsMkvVideoUploadUrl() throws Exception {
        String auth = "Bearer " + register("kira@example.com");

        // .mkv (Matroska) képernyőfelvételhez — feltölthető (a böngészős lejátszás kodekfüggő).
        mockMvc.perform(post("/api/posts/media/upload-url")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"video/x-matroska"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(org.hamcrest.Matchers.endsWith(".mkv")));
    }

    private String createPost(String auth, String content) throws Exception {
        var result = mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\"}".formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void editOwnPostText() throws Exception {
        String auth = "Bearer " + register("liv@example.com");
        String id = createPost(auth, "Eredeti szöveg");

        mockMvc.perform(patch("/api/posts/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Javított szöveg"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.content").value("Javított szöveg"));

        // A listában is a javított szöveg jelenik meg.
        mockMvc.perform(get("/api/posts/me").header("Authorization", auth))
                .andExpect(jsonPath("$[0].content").value("Javított szöveg"));
    }

    @Test
    void rejectsEditAndDeleteByNonAuthor() throws Exception {
        String owner = "Bearer " + register("owner@example.com");
        String other = "Bearer " + register("other@example.com");
        String id = createPost(owner, "A tulaj posztja");

        // Idegen nem szerkesztheti (létezést sem szivárogtatunk → 404).
        mockMvc.perform(patch("/api/posts/" + id).header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"belenyúlok"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));

        // Idegen nem törölheti.
        mockMvc.perform(delete("/api/posts/" + id).header("Authorization", other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));

        // A poszt érintetlen.
        mockMvc.perform(get("/api/posts/me").header("Authorization", owner))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("A tulaj posztja"));
    }

    @Test
    void deleteOwnPost() throws Exception {
        String auth = "Bearer " + register("zara@example.com");
        String id = createPost(auth, "Törlendő poszt");

        mockMvc.perform(delete("/api/posts/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/me").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
