package com.nexa.comment;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentFlowTest {

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
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String profilePost(String auth, String content) throws Exception {
        var result = mockMvc.perform(post("/api/posts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\",\"media\":[]}".formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String comment(String auth, String postId, String content, String parentId) throws Exception {
        String body = parentId == null
                ? "{\"content\":\"%s\"}".formatted(content)
                : "{\"content\":\"%s\",\"parentId\":\"%s\"}".formatted(content, parentId);
        var result = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void commentReplyEditAndModerationFlow() throws Exception {
        String authA = register("cm-ann@example.com", "Ann");
        String authB = register("cm-bob@example.com", "Bob");

        String postId = profilePost(authA, "Ann bejegyzése");

        // B hozzászól, A válaszol rá → fa: 1 hozzászólás, alatta 1 válasz.
        String c1 = comment(authB, postId, "Szuper poszt!", null);
        comment(authA, postId, "Köszönöm!", c1);

        mockMvc.perform(get("/api/posts/" + postId + "/comments").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Szuper poszt!"))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].editedAt").doesNotExist())
                .andExpect(jsonPath("$[0].replies.length()").value(1))
                .andExpect(jsonPath("$[0].replies[0].content").value("Köszönöm!"))
                .andExpect(jsonPath("$[0].replies[0].parentId").value(c1));

        // B szerkeszti a saját hozzászólását → editedAt kitöltve.
        mockMvc.perform(patch("/api/comments/" + c1)
                        .header("Authorization", authB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Szuper poszt! (javítva)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Szuper poszt! (javítva)"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());

        // Idegen nem szerkesztheti B hozzászólását.
        mockMvc.perform(patch("/api/comments/" + c1)
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hack\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        // A (a bejegyzés szerzője) moderálhatja: törli B hozzászólását → a válasz is eltűnik.
        mockMvc.perform(delete("/api/comments/" + c1).header("Authorization", authA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/" + postId + "/comments").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void inGroupOnlyAdminModeratesCommentsNotThePostAuthor() throws Exception {
        String authAdmin = register("cm-eve@example.com", "Eve");
        String authBob = register("cm-finn@example.com", "Finn");
        String authCara = register("cm-gwen@example.com", "Gwen");

        // Admin csoportot hoz létre; Finn és Gwen csatlakozik.
        var gres = mockMvc.perform(post("/api/groups")
                        .header("Authorization", authAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Moderációs csoport\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String gid = objectMapper.readTree(gres.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", authBob))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", authCara))
                .andExpect(status().isOk());

        // Finn (NEM admin) posztol a csoportba; Gwen hozzászól.
        var pres = mockMvc.perform(post("/api/groups/" + gid + "/posts")
                        .header("Authorization", authBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Finn csoportposztja\",\"media\":[]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String postId = objectMapper.readTree(pres.getResponse().getContentAsString()).get("id").asText();
        String cid = comment(authCara, postId, "Gwen hozzászólása", null);

        // Finn a poszt szerzője, DE nem admin → NEM törölheti Gwen hozzászólását.
        mockMvc.perform(delete("/api/comments/" + cid).header("Authorization", authBob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        // A csoport admin viszont moderálhatja.
        mockMvc.perform(delete("/api/comments/" + cid).header("Authorization", authAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/" + postId + "/comments").header("Authorization", authAdmin))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void commentCanCarryMediaAndMediaOnlyIsAllowed() throws Exception {
        String authA = register("cm-ivy@example.com", "Ivy");
        String postId = profilePost(authA, "Ivy bejegyzése");

        // Szöveg nélküli, csak médiát tartalmazó hozzászólás is mehet.
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"","media":[{"key":"posts/kep.jpg","type":"IMAGE","sizeBytes":1234}]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.media.length()").value(1))
                .andExpect(jsonPath("$.media[0].type").value("IMAGE"))
                .andExpect(jsonPath("$.media[0].url").value(org.hamcrest.Matchers.containsString("kep.jpg")));

        // A komment-fában is visszajön a média.
        mockMvc.perform(get("/api/posts/" + postId + "/comments").header("Authorization", authA))
                .andExpect(jsonPath("$[0].media.length()").value(1));

        // Nem a média-mappába mutató kulcs → INVALID_UPLOAD.
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"x","media":[{"key":"hack/evil.jpg","type":"IMAGE","sizeBytes":1}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
    }

    @Test
    void onlyGroupMembersCanCommentOnGroupPost() throws Exception {
        String authA = register("cm-cara@example.com", "Cara");
        String authB = register("cm-dan@example.com", "Dan");

        // A csoportot hoz létre és posztol bele.
        var gres = mockMvc.perform(post("/api/groups")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Komment csoport\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String gid = objectMapper.readTree(gres.getResponse().getContentAsString()).get("id").asText();
        var pres = mockMvc.perform(post("/api/groups/" + gid + "/posts")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Csoportposzt\",\"media\":[]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String postId = objectMapper.readTree(pres.getResponse().getContentAsString()).get("id").asText();

        // B nem tag → nem kommentelhet.
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", authB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"kívülről\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        // Csatlakozás után már igen.
        mockMvc.perform(post("/api/groups/" + gid + "/join").header("Authorization", authB))
                .andExpect(status().isOk());
        comment(authB, postId, "Most már tag vagyok", null);
        mockMvc.perform(get("/api/posts/" + postId + "/comments").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
