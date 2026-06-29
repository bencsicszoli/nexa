package com.nexa.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.support.TestSubscriptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A hírfolyam (#10) végpont integrációs tesztje: a folyam pontosan az ismerősök +
 * követettek profil-posztjait és a tag-csoportok posztjait mutatja, időrendben
 * (legfrissebb felül), és helyesen lapoz a cursorral.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedFlowTest {

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
        String token = body.get("accessToken").asText();
        TestSubscriptions.grantActive(mockMvc, "Bearer " + token);
        return new String[]{token, body.get("user").get("id").asText()};
    }

    /** Profil-bejegyzés létrehozása az adott felhasználóval. */
    private void createPost(String auth, String content) throws Exception {
        mockMvc.perform(post("/api/posts").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\",\"media\":[]}".formatted(content)))
                .andExpect(status().isCreated());
    }

    /** Kétirányú ismerőssé teszi a két felhasználót (kérés + elfogadás). */
    private void befriend(String authA, String idA, String authB, String idB) throws Exception {
        mockMvc.perform(post("/api/friends/requests").header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(idB)))
                .andExpect(status().isCreated());
        var reqResult = mockMvc.perform(get("/api/friends/requests").header("Authorization", authB))
                .andReturn();
        String requestId = objectMapper.readTree(reqResult.getResponse().getContentAsString())
                .get("incoming").get(0).get("requestId").asText();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", authB))
                .andExpect(status().isNoContent());
    }

    /** Új csoport, a hívó az admin; visszaadja a csoport id-ját. */
    private String createGroup(String auth, String name) throws Exception {
        var result = mockMvc.perform(post("/api/groups").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"description\":\"\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /** A feed egy lapja: visszaadja a JSON gyökeret (items + nextCursor). */
    private JsonNode getFeed(String auth, String queryString) throws Exception {
        var result = mockMvc.perform(get("/api/feed" + queryString).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Set<String> contentsOf(JsonNode items) {
        Set<String> contents = new HashSet<>();
        items.forEach(p -> contents.add(p.get("content").asText()));
        return contents;
    }

    @Test
    void feedShowsFriendsFollowedAndGroupPostsNewestFirstAndExcludesOthers() throws Exception {
        String[] a = register("alice@feed.com", "Alice");
        String[] b = register("bob@feed.com", "Bob");
        String[] c = register("cara@feed.com", "Cara");
        String[] d = register("dan@feed.com", "Dan");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];
        String authC = "Bearer " + c[0];
        String authD = "Bearer " + d[0];

        // Új fióknak még üres a hírfolyama.
        JsonNode empty = getFeed(authA, "");
        assertThat(empty.get("items")).isEmpty();
        assertThat(empty.get("nextCursor").isNull()).isTrue();

        // A ↔ B ismerősök; A követi C-t; D idegen marad.
        befriend(authA, a[1], authB, b[1]);
        mockMvc.perform(put("/api/follows/" + c[1]).header("Authorization", authA))
                .andExpect(status().isNoContent());

        // A létrehoz egy csoportot (tag-csoport) és posztol bele.
        String groupId = createGroup(authA, "Hikers");

        // Bejegyzések — a létrehozás sorrendje a kronológia (a legutóbbi a legfrissebb).
        createPost(authA, "saját profilposzt");                 // A profilja → NEM a feedbe
        createPost(authB, "bob-1");                              // ismerős → feedbe
        createPost(authC, "cara-1");                             // követett → feedbe
        createPost(authD, "dan-titok");                          // idegen → NEM a feedbe
        createPost(authB, "bob-2");                              // ismerős → feedbe
        mockMvc.perform(post("/api/groups/" + groupId + "/posts").header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"csoportposzt\",\"media\":[]}"))
                .andExpect(status().isCreated());               // tag-csoport → feedbe

        JsonNode feed = getFeed(authA, "");
        JsonNode items = feed.get("items");

        // Pontosan a négy várt bejegyzés, az idegen és a saját profilposzt nélkül.
        assertThat(contentsOf(items))
                .containsExactlyInAnyOrder("bob-1", "cara-1", "bob-2", "csoportposzt");
        assertThat(contentsOf(items)).doesNotContain("saját profilposzt", "dan-titok");

        // A csoportposzt jelöli a forráscsoportot; a profil-posztoké null.
        items.forEach(p -> {
            if (p.get("content").asText().equals("csoportposzt")) {
                assertThat(p.get("group").get("name").asText()).isEqualTo("Hikers");
            } else {
                assertThat(p.get("group").isNull()).isTrue();
            }
        });

        // Időrend: a createdAt szigorúan nem növekvő (legfrissebb felül).
        List<Instant> times = new ArrayList<>();
        items.forEach(p -> times.add(Instant.parse(p.get("createdAt").asText())));
        for (int i = 1; i < times.size(); i++) {
            assertThat(times.get(i)).isBeforeOrEqualTo(times.get(i - 1));
        }
        // Egy teljes lapnál (alap limit) nincs több — nincs következő cursor.
        assertThat(feed.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void feedPaginatesWithCursorWithoutGapsOrDuplicates() throws Exception {
        String[] a = register("erin@feed.com", "Erin");
        String[] b = register("finn@feed.com", "Finn");
        String authA = "Bearer " + a[0];
        String authB = "Bearer " + b[0];
        befriend(authA, a[1], authB, b[1]);

        // Négy bejegyzés a baráttól → két, egyenként kétméretű lap.
        createPost(authB, "p1");
        createPost(authB, "p2");
        createPost(authB, "p3");
        createPost(authB, "p4");

        JsonNode page1 = getFeed(authA, "?limit=2");
        assertThat(page1.get("items")).hasSize(2);
        assertThat(page1.get("nextCursor").isNull()).isFalse();
        Set<String> seen = new HashSet<>(contentsOf(page1.get("items")));

        String cursor = page1.get("nextCursor").asText();
        JsonNode page2 = getFeed(authA, "?limit=2&cursor=" + cursor);
        assertThat(page2.get("items")).hasSize(2);

        // A két lap diszjunkt, és együtt a teljes négy bejegyzés (nincs kihagyás/ismétlés).
        Set<String> page2Contents = contentsOf(page2.get("items"));
        assertThat(seen).doesNotContainAnyElementsOf(page2Contents);
        seen.addAll(page2Contents);
        assertThat(seen).containsExactlyInAnyOrder("p1", "p2", "p3", "p4");

        // Pontosan négy elem után nincs több lap.
        assertThat(page2.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void invalidCursorIsRejectedAndFeedRequiresAuth() throws Exception {
        String[] a = register("gina@feed.com", "Gina");
        String authA = "Bearer " + a[0];

        // Sérült cursor → 400 INVALID_CURSOR.
        mockMvc.perform(get("/api/feed?cursor=not-a-valid-cursor").header("Authorization", authA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));

        // Token nélkül védett.
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized());
    }
}
