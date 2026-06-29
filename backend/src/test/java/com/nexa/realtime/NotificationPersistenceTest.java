package com.nexa.realtime;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Az értesítés-perzisztálás (#17) flow-tesztje: kapcsolati események (ismerőskérés, elfogadás,
 * új követő) tartós értesítést hagynak az előzményben, olvasatlan-számmal; az olvasott-jelölés
 * nullázza; a kikapcsolt preferencia pedig meg sem jelenik.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationPersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Regisztrál + aktív előfizetést ad (a friend/follow végpontok gateltek). Visszaad: [auth, userId]. */
    private String[] register(String email, String name) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"%s","password":"supersecret"}"""
                                .formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String auth = "Bearer " + body.get("accessToken").asText();
        TestSubscriptions.grantActive(mockMvc, auth);
        return new String[]{auth, body.get("user").get("id").asText()};
    }

    private JsonNode notifications(String auth) throws Exception {
        var res = mockMvc.perform(get("/api/notifications").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private long unread(String auth) throws Exception {
        var res = mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("count").asLong();
    }

    @Test
    void relationshipEventsPersistAndCanBeRead() throws Exception {
        String[] a = register("np-a@example.com", "Anna");
        String[] b = register("np-b@example.com", "Bence");
        String[] c = register("np-c@example.com", "Cili");

        // A → B ismerőskérés: B-nél FRIEND_REQUEST (olvasatlan).
        mockMvc.perform(post("/api/friends/requests").header("Authorization", a[0])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(b[1])))
                .andExpect(status().isCreated());

        JsonNode bNotifs = notifications(b[0]);
        org.assertj.core.api.Assertions.assertThat(bNotifs.get("items").get(0).get("type").asText())
                .isEqualTo("FRIEND_REQUEST");
        org.assertj.core.api.Assertions.assertThat(bNotifs.get("items").get(0).get("actorId").asText())
                .isEqualTo(a[1]);
        org.assertj.core.api.Assertions.assertThat(bNotifs.get("items").get(0).get("read").asBoolean())
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(unread(b[0])).isEqualTo(1);

        // B elfogadja → A-nál FRIEND_ACCEPTED (az aktor B).
        String requestId = objectMapper.readTree(
                        mockMvc.perform(get("/api/friends/requests").header("Authorization", b[0]))
                                .andReturn().getResponse().getContentAsString())
                .get("incoming").get(0).get("requestId").asText();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", b[0]))
                .andExpect(status().isNoContent());

        JsonNode aNotifs = notifications(a[0]);
        org.assertj.core.api.Assertions.assertThat(aNotifs.get("items").get(0).get("type").asText())
                .isEqualTo("FRIEND_ACCEPTED");
        org.assertj.core.api.Assertions.assertThat(aNotifs.get("items").get(0).get("actorId").asText())
                .isEqualTo(b[1]);

        // C követi A-t → A-nál NEW_FOLLOWER (legfrissebb felül).
        mockMvc.perform(put("/api/follows/" + a[1]).header("Authorization", c[0]))
                .andExpect(status().isNoContent());

        JsonNode aNotifs2 = notifications(a[0]);
        org.assertj.core.api.Assertions.assertThat(aNotifs2.get("items").get(0).get("type").asText())
                .isEqualTo("NEW_FOLLOWER");
        org.assertj.core.api.Assertions.assertThat(aNotifs2.get("items").get(0).get("actorId").asText())
                .isEqualTo(c[1]);
        org.assertj.core.api.Assertions.assertThat(unread(a[0])).isEqualTo(2);

        // Mind olvasott → 0.
        mockMvc.perform(post("/api/notifications/read-all").header("Authorization", a[0]))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(unread(a[0])).isEqualTo(0);
    }

    @Test
    void disabledPreferenceIsNotPersisted() throws Exception {
        String[] a = register("np-d@example.com", "Dani");
        String[] d = register("np-e@example.com", "Eszter");

        // D kikapcsolja az ismerőskérés-értesítést.
        mockMvc.perform(patch("/api/settings/notifications").header("Authorization", d[0])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPost":true,"friendRequest":false,"friendAccepted":true,"newFollower":true}"""))
                .andExpect(status().isOk());

        // A → D ismerőskérés: D-nél nem keletkezik értesítés.
        mockMvc.perform(post("/api/friends/requests").header("Authorization", a[0])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(d[1])))
                .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(notifications(d[0]).get("items").size()).isZero();
        org.assertj.core.api.Assertions.assertThat(unread(d[0])).isEqualTo(0);
    }
}
