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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Privát csoport csatlakozási kérelmek + admin-moderáció (kizárás, poszt-törlés). */
@SpringBootTest
@AutoConfigureMockMvc
class GroupModerationTest {

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
        String authHeader = "Bearer " + body.get("accessToken").asText();
        TestSubscriptions.grantActive(mockMvc, authHeader);
        return new String[]{authHeader, body.get("user").get("id").asText()};
    }

    private String createGroup(String auth, String name, String visibility) throws Exception {
        var result = mockMvc.perform(post("/api/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"visibility\":\"%s\"}".formatted(name, visibility)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value(visibility))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String groupPost(String auth, String groupId, String content) throws Exception {
        var result = mockMvc.perform(post("/api/groups/" + groupId + "/posts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\",\"media\":[]}".formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void privateGroupJoinRequiresApproval() throws Exception {
        String[] admin = register("mod-ann@example.com", "Ann");
        String[] bob = register("mod-bob@example.com", "Bob");
        String gid = createGroup(admin[0], "Privát klub", "PRIVATE");

        // Bob „csatlakozik" → privátnál ez kérelem: nem tag, de requested.
        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", bob[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.requested").value(true))
                .andExpect(jsonPath("$.memberCount").value(1));

        // Tagság híján nem posztolhat.
        mockMvc.perform(post("/api/groups/" + gid + "/posts")
                        .header("Authorization", bob[0])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\",\"media\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        // Az admin látja a függő kérelmet.
        mockMvc.perform(get("/api/groups/" + gid + "/requests").header("Authorization", admin[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(bob[1]))
                .andExpect(jsonPath("$[0].displayName").value("Bob"));

        // Nem-admin nem láthatja a kérelmeket.
        mockMvc.perform(get("/api/groups/" + gid + "/requests").header("Authorization", bob[0]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_ADMIN"));

        // Jóváhagyás → Bob taggá válik, posztolhat.
        mockMvc.perform(post("/api/groups/" + gid + "/requests/" + bob[1] + "/approve")
                        .header("Authorization", admin[0]))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/groups/" + gid).header("Authorization", bob[0]))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(2));
        groupPost(bob[0], gid, "Most már bent vagyok");
    }

    @Test
    void rejectJoinRequestKeepsUserOut() throws Exception {
        String[] admin = register("mod-cara@example.com", "Cara");
        String[] dan = register("mod-dan@example.com", "Dan");
        String gid = createGroup(admin[0], "Zárt kör", "PRIVATE");

        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", dan[0]))
                .andExpect(jsonPath("$.requested").value(true));
        mockMvc.perform(delete("/api/groups/" + gid + "/requests/" + dan[1])
                        .header("Authorization", admin[0]))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/groups/" + gid).header("Authorization", dan[0]))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.requested").value(false));
    }

    @Test
    void adminKicksMemberButPostsRemain() throws Exception {
        String[] admin = register("mod-eve@example.com", "Eve");
        String[] finn = register("mod-finn@example.com", "Finn");
        String gid = createGroup(admin[0], "Nyilvános tér", "PUBLIC");

        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", finn[0]))
                .andExpect(jsonPath("$.role").value("MEMBER"));
        groupPost(finn[0], gid, "Finn bejegyzése");

        // Nem-admin nem zárhat ki.
        mockMvc.perform(delete("/api/groups/" + gid + "/members/" + admin[1])
                        .header("Authorization", finn[0]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_ADMIN"));

        // Admin nem zárhatja ki saját magát (kilépni a /leave-vel lehet).
        mockMvc.perform(delete("/api/groups/" + gid + "/members/" + admin[1])
                        .header("Authorization", admin[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_KICK_SELF"));

        // Admin kizárja Finnt.
        mockMvc.perform(delete("/api/groups/" + gid + "/members/" + finn[1])
                        .header("Authorization", admin[0]))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/groups/" + gid).header("Authorization", admin[0]))
                .andExpect(jsonPath("$.memberCount").value(1));

        // Finn posztja MEGMARAD.
        mockMvc.perform(get("/api/groups/" + gid + "/posts").header("Authorization", admin[0]))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Finn bejegyzése"));
    }

    @Test
    void adminDeletesGroupPost() throws Exception {
        String[] admin = register("mod-gwen@example.com", "Gwen");
        String[] hugo = register("mod-hugo@example.com", "Hugo");
        String gid = createGroup(admin[0], "Moderált csoport", "PUBLIC");
        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", hugo[0]));
        String postId = groupPost(hugo[0], gid, "Oda nem illő");

        // Másik tag (nem szerző, nem admin) nem törölheti — itt az adminon kívül nincs ilyen,
        // ezért a fő eset: az admin moderál.
        mockMvc.perform(delete("/api/groups/" + gid + "/posts/" + postId)
                        .header("Authorization", admin[0]))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/groups/" + gid + "/posts").header("Authorization", admin[0]))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
