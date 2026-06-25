package com.nexa.friend;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FriendFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Regisztrál egy felhasználót, és visszaadja az [accessToken, userId] párt. */
    private String[] register(String email, String name) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"%s","password":"supersecret"}"""
                                .formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new String[]{body.get("accessToken").asText(), body.get("user").get("id").asText()};
    }

    private String sendRequest(String auth, String targetUserId) throws Exception {
        mockMvc.perform(post("/api/friends/requests").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(targetUserId)))
                .andExpect(status().isCreated());
        // A kérés azonosítója a címzett "incoming" listájáról olvasható ki.
        return null;
    }

    @Test
    void fullFriendshipHappyPath() throws Exception {
        String[] a = register("anna@example.com", "Anna");
        String[] b = register("bence@example.com", "Bence");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];
        String idB = b[1];

        // Kezdetben egyik listán sincs senki.
        mockMvc.perform(get("/api/friends").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // A → B kérés.
        sendRequest(authA, idB);

        // A kimenő kérése megjelenik nála, B-nél pedig beérkezőként.
        mockMvc.perform(get("/api/friends/requests").header("Authorization", authA))
                .andExpect(jsonPath("$.outgoing.length()").value(1))
                .andExpect(jsonPath("$.outgoing[0].displayName").value("Bence"))
                .andExpect(jsonPath("$.incoming.length()").value(0));

        var reqResult = mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                .andExpect(jsonPath("$.incoming.length()").value(1))
                .andExpect(jsonPath("$.incoming[0].displayName").value("Anna"))
                .andReturn();
        String requestId = objectMapper.readTree(reqResult.getResponse().getContentAsString())
                .get("incoming").get(0).get("requestId").asText();

        // B elfogadja.
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", authB))
                .andExpect(status().isNoContent());

        // Mindkettő ismerőslistáján megjelenik a másik.
        mockMvc.perform(get("/api/friends").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Bence"));
        mockMvc.perform(get("/api/friends").header("Authorization", authB))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Anna"));

        // A függő kérések kiürültek.
        mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                .andExpect(jsonPath("$.incoming.length()").value(0));
    }

    @Test
    void browsePeopleReflectsRelationship() throws Exception {
        String[] a = register("clara@example.com", "Clara");
        String[] b = register("david@example.com", "David");
        String authA = "Bearer " + a[0];

        // Kezdetben David kapcsolata NONE.
        mockMvc.perform(get("/api/friends/people").header("Authorization", authA)
                        .param("query", "David"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("David"))
                .andExpect(jsonPath("$[0].relationship").value("NONE"));

        sendRequest(authA, b[1]);

        // Kérés után REQUEST_SENT, kitöltött requestId-vel.
        mockMvc.perform(get("/api/friends/people").header("Authorization", authA)
                        .param("query", "David"))
                .andExpect(jsonPath("$[0].relationship").value("REQUEST_SENT"))
                .andExpect(jsonPath("$[0].requestId").isNotEmpty());

        // A saját magát a böngészés kizárja.
        mockMvc.perform(get("/api/friends/people").header("Authorization", authA)
                        .param("query", "Clara"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsSelfAndDuplicateRequests() throws Exception {
        String[] a = register("emma@example.com", "Emma");
        String[] b = register("finn@example.com", "Finn");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];

        // Saját magának nem küldhet.
        mockMvc.perform(post("/api/friends/requests").header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(a[1])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_FRIEND_REQUEST"));

        sendRequest(authA, b[1]);

        // Ugyanannak újra → már elküldve.
        mockMvc.perform(post("/api/friends/requests").header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(b[1])))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FRIEND_REQUEST_ALREADY_SENT"));

        // Fordított irányban (B → A) sem indítható új kérés, a meglévőt kell elfogadni.
        mockMvc.perform(post("/api/friends/requests").header("Authorization", authB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(a[1])))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVERSE_FRIEND_REQUEST_EXISTS"));
    }

    @Test
    void declineRequestThenResendAndRemoveFriend() throws Exception {
        String[] a = register("gabi@example.com", "Gabi");
        String[] b = register("hugo@example.com", "Hugo");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];

        sendRequest(authA, b[1]);
        String requestId = objectMapper.readTree(
                        mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                                .andReturn().getResponse().getContentAsString())
                .get("incoming").get(0).get("requestId").asText();

        // B elutasítja → a rekord törlődik, így A újraküldhet.
        mockMvc.perform(delete("/api/friends/requests/" + requestId).header("Authorization", authB))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                .andExpect(jsonPath("$.incoming.length()").value(0));

        // Újraküldés, majd elfogadás.
        sendRequest(authA, b[1]);
        String requestId2 = objectMapper.readTree(
                        mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                                .andReturn().getResponse().getContentAsString())
                .get("incoming").get(0).get("requestId").asText();
        mockMvc.perform(post("/api/friends/requests/" + requestId2 + "/accept").header("Authorization", authB))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/friends").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(1));

        // A eltávolítja Hugót → mindkét listáról eltűnik.
        mockMvc.perform(delete("/api/friends/" + b[1]).header("Authorization", authA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/friends").header("Authorization", authA))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/friends").header("Authorization", authB))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotAcceptSomeoneElsesRequest() throws Exception {
        String[] a = register("iris@example.com", "Iris");
        String[] b = register("jack@example.com", "Jack");
        String[] c = register("kate@example.com", "Kate");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];
        String authC = "Bearer " + c[0];

        sendRequest(authA, b[1]);
        String requestId = objectMapper.readTree(
                        mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                                .andReturn().getResponse().getContentAsString())
                .get("incoming").get(0).get("requestId").asText();

        // Kívülálló (Kate) nem fogadhatja el más kérését (létezést sem szivárogtatunk → 404).
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept").header("Authorization", authC))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIEND_REQUEST_NOT_FOUND"));

        // Token nélkül védett.
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }
}
