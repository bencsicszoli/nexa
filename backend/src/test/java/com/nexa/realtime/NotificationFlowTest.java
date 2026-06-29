package com.nexa.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.realtime.dto.NotificationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Az #11 végpontok közötti („két böngésző") integrációs tesztje valódi szerveren és valódi
 * STOMP-kliensekkel: A és B ismerősök, B felcsatlakozik és feliratkozik az értesítéseire,
 * majd A posztol — B-nek meg kell kapnia a {@code NEW_POST} értesítést. Egy nem-kapcsolat
 * (C) ugyanerre a posztra <b>nem</b> kap értesítést.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationFlowTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    private final RestTemplate rest = new RestTemplate();

    /** Regisztrál egy felhasználót, és visszaadja az [accessToken, userId] párt. */
    private String[] register(String email, String name) {
        String body = """
                {"email":"%s","displayName":"%s","password":"supersecret"}""".formatted(email, name);
        JsonNode node = postJson("/api/auth/register", body, null);
        String token = node.get("accessToken").asText();
        // Gating (#15): a teszt-userek aktív előfizetést kapnak a premium végpontokhoz.
        postJson("/api/dev/subscription", "{\"status\":\"ACTIVE\"}", token);
        return new String[]{token, node.get("user").get("id").asText()};
    }

    private void befriend(String authA, String authB, String idB) throws Exception {
        postJson("/api/friends/requests", "{\"userId\":\"%s\"}".formatted(idB), authA);
        JsonNode reqs = getJson("/api/friends/requests", authB);
        String requestId = reqs.get("incoming").get(0).get("requestId").asText();
        postJson("/api/friends/requests/" + requestId + "/accept", "", authB);
    }

    private JsonNode postJson(String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        String resp = rest.postForObject(url(path), new HttpEntity<>(body, headers), String.class);
        return readTree(resp);
    }

    private JsonNode getJson(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        String resp = rest.exchange(url(path), org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getBody();
        return readTree(resp);
    }

    private JsonNode readTree(String json) {
        try {
            return json == null || json.isBlank() ? objectMapper.createObjectNode()
                    : objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    /** Felcsatlakozik STOMP-pal a megadott tokennel, és feliratkozik az értesítésekre. */
    private BlockingQueue<NotificationDto> connectAndSubscribe(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        // A Spring Boot ObjectMapper-jét használjuk (JSR310 modullal), hogy az Instant mező
        // deszerializálódjon — a böngészős JS-kliensnek erre nincs szüksége.
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<NotificationDto> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((NotificationDto) payload);
            }
        });
        // A SUBSCRIBE keret aszinkron; rövid várakozás, hogy a szerver biztosan regisztrálja.
        Thread.sleep(400);
        return received;
    }

    @Test
    void friendReceivesNotification_strangerDoesNot() throws Exception {
        String[] a = register("notif-a@example.com", "Anna");
        String[] b = register("notif-b@example.com", "Bela");
        String[] c = register("notif-c@example.com", "Cili");
        befriend(a[0], b[0], b[1]);

        BlockingQueue<NotificationDto> bInbox = connectAndSubscribe(b[0]);
        BlockingQueue<NotificationDto> cInbox = connectAndSubscribe(c[0]);

        postJson("/api/posts", "{\"content\":\"Sziasztok!\",\"media\":[]}", a[0]);

        NotificationDto notif = bInbox.poll(5, TimeUnit.SECONDS);
        assertThat(notif).isNotNull();
        assertThat(notif.type()).isEqualTo(NotificationDto.TYPE_NEW_POST);
        assertThat(notif.actorId()).isEqualTo(a[1]);
        assertThat(notif.actorName()).isEqualTo("Anna");
        assertThat(notif.postId()).isNotBlank();
        assertThat(notif.groupId()).isNull();

        // A nem-kapcsolat semmit nem kap.
        assertThat(cInbox.poll(1, TimeUnit.SECONDS)).isNull();
    }
}
