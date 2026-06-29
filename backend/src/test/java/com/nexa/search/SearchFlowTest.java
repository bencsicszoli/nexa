package com.nexa.search;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A keresés (#16) integrációs tesztje: egyetlen kifejezés mindhárom típusra (felhasználó,
 * csoport, bejegyzés) ad találatot, a privát csoport posztja nem szivárog ki nem-tagnak, és az
 * üres kifejezés üres találatot ad. Az adatok közös (osztályszintű) H2-ben élnek, ezért minden
 * teszt egyedi, jól kereshető tokent használ, hogy a más tesztek adata ne zavarjon.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchFlowTest {

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
        String token = body.get("accessToken").asText();
        TestSubscriptions.grantActive(mockMvc, "Bearer " + token);
        return token;
    }

    private String createGroup(String auth, String name, String visibility) throws Exception {
        var result = mockMvc.perform(post("/api/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"description\":\"\",\"visibility\":\"%s\"}"
                                .formatted(name, visibility)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void writePost(String auth, String content) throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\",\"media\":[]}".formatted(content)))
                .andExpect(status().isCreated());
    }

    @Test
    void searchFindsUsersGroupsAndPosts() throws Exception {
        String authA = "Bearer " + register("search-anna@example.com", "Findmeseven Anna");
        String authB = "Bearer " + register("search-bob@example.com", "Bob Bystander");

        writePost(authA, "Findmeseven napi gondolat");
        createGroup(authA, "Findmeseven klub", "PUBLIC");

        // Bob keres az egyedi tokenre → mindhárom típusra pontosan egy találat.
        mockMvc.perform(get("/api/search?q=Findmeseven").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.users[0].displayName").value("Findmeseven Anna"))
                .andExpect(jsonPath("$.groups.length()").value(1))
                .andExpect(jsonPath("$.groups[0].name").value("Findmeseven klub"))
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].content").value("Findmeseven napi gondolat"));

        // A keresés a hívót kizárja a felhasználó-találatokból (Bob nem találja saját magát).
        mockMvc.perform(get("/api/search?q=Bystander").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(0));
    }

    @Test
    void privateGroupPostDoesNotLeakToNonMember() throws Exception {
        String authA = "Bearer " + register("search-cara@example.com", "Cara");
        String authB = "Bearer " + register("search-dan@example.com", "Dan");

        String groupId = createGroup(authA, "Titkos Secretxyz", "PRIVATE");
        mockMvc.perform(post("/api/groups/" + groupId + "/posts")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Secretxyz suttogás\",\"media\":[]}"))
                .andExpect(status().isCreated());

        // A tag (Cara) megtalálja a privát csoport posztját.
        mockMvc.perform(get("/api/search?q=Secretxyz suttogás").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1));

        // Dan (nem tag) NEM kapja meg a privát csoport posztját (adatvédelmi szűrés).
        mockMvc.perform(get("/api/search?q=Secretxyz suttogás").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void blankQueryIsEmptyAndUnauthenticatedIsRejected() throws Exception {
        String auth = "Bearer " + register("search-eve@example.com", "Eve");

        mockMvc.perform(get("/api/search?q=").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(0))
                .andExpect(jsonPath("$.groups.length()").value(0))
                .andExpect(jsonPath("$.posts.length()").value(0));

        // Token nélkül védett.
        mockMvc.perform(get("/api/search?q=anything"))
                .andExpect(status().isUnauthorized());
    }
}
