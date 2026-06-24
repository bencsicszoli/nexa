package com.nexa.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void rejectsUnauthenticatedAndBlankContent() throws Exception {
        // Token nélkül védett.
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"akármi"}"""))
                .andExpect(status().isUnauthorized());

        String auth = "Bearer " + register("erik@example.com");

        // Üres tartalom → validációs hiba.
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
