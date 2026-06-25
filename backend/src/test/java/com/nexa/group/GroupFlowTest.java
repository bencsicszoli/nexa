package com.nexa.group;

import com.fasterxml.jackson.databind.JsonNode;
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
class GroupFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Regisztrál egy felhasználót, és visszaadja az accessTokent. */
    private String register(String email, String name) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"%s","password":"supersecret"}"""
                                .formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    /** Létrehoz egy csoportot a megadott tokennel, és visszaadja az azonosítóját. */
    private String createGroup(String auth, String name) throws Exception {
        var result = mockMvc.perform(post("/api/groups")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"description\":\"\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createJoinAndPostInGroupFlow() throws Exception {
        String authA = "Bearer " + register("grp-ann@example.com", "Ann");
        String authB = "Bearer " + register("grp-bob@example.com", "Bob");

        String groupId = createGroup(authA, "Fotósok köre");

        // B böngész (névre szűrve, mert a DB az osztály tesztjei közt megosztott):
        // látja a csoportot, még nem tag (role null).
        mockMvc.perform(get("/api/groups?query=Fotósok köre").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Fotósok köre"))
                .andExpect(jsonPath("$[0].role").doesNotExist());

        // B csatlakozik → tag lesz, a létszám 2.
        mockMvc.perform(post("/api/groups/" + groupId + "/join").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(2));

        // B posztol a csoportba.
        mockMvc.perform(post("/api/groups/" + groupId + "/posts")
                        .header("Authorization", authB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Sziasztok!\",\"media\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Sziasztok!"))
                .andExpect(jsonPath("$.authorName").value("Bob"));

        // A csoport posztlistája tartalmazza a posztot (A is látja).
        mockMvc.perform(get("/api/groups/" + groupId + "/posts").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Sziasztok!"));

        // A csoport-poszt NEM jelenik meg B profil-időrendjén.
        mockMvc.perform(get("/api/posts/me").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // B „Csoportjaim" listáján ott a csoport.
        mockMvc.perform(get("/api/groups/mine").header("Authorization", authB))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(groupId));
    }

    @Test
    void nonMemberCannotPostAndUnknownGroupIs404() throws Exception {
        String authA = "Bearer " + register("grp-carl@example.com", "Carl");
        String authB = "Bearer " + register("grp-dora@example.com", "Dora");
        String groupId = createGroup(authA, "Kódolás HU");

        // B nem tag → nem posztolhat (403).
        mockMvc.perform(post("/api/groups/" + groupId + "/posts")
                        .header("Authorization", authB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\",\"media\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        // Nem létező csoport → 404.
        mockMvc.perform(get("/api/groups/00000000-0000-0000-0000-000000000000/posts")
                        .header("Authorization", authA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));

        // Token nélkül védett.
        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lastAdminCannotLeaveWhileOthersRemain() throws Exception {
        String authA = "Bearer " + register("grp-ed@example.com", "Ed");
        String authB = "Bearer " + register("grp-fay@example.com", "Fay");
        String groupId = createGroup(authA, "Receptek");

        mockMvc.perform(post("/api/groups/" + groupId + "/join").header("Authorization", authB))
                .andExpect(status().isOk());

        // A egyetlen admin, és van másik tag → nem léphet ki.
        mockMvc.perform(post("/api/groups/" + groupId + "/leave").header("Authorization", authA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GROUP_LAST_ADMIN"));

        // B (sima tag) viszont kiléphet.
        mockMvc.perform(post("/api/groups/" + groupId + "/leave").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.memberCount").value(1));

        // Most A egyedüli tag → kiléphet.
        mockMvc.perform(post("/api/groups/" + groupId + "/leave").header("Authorization", authA))
                .andExpect(status().isOk());
    }

    @Test
    void groupNameIsRequired() throws Exception {
        String authA = "Bearer " + register("grp-gwen@example.com", "Gwen");
        mockMvc.perform(post("/api/groups")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
