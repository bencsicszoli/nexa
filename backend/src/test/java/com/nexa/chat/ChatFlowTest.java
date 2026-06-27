package com.nexa.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.chat.dto.ChatMessageDto;
import com.nexa.chat.dto.SendMessageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Az #12 végpontok közötti („két felhasználó") integrációs tesztje valódi szerveren és valódi
 * STOMP-kliensekkel: A megnyit egy 1:1 szálat B-vel, B felcsatlakozik és feliratkozik az
 * üzeneteire, A elküld egy üzenetet STOMP-on — B-nek élőben meg kell kapnia. Ellenőrizzük, hogy
 * az előzmény perzisztálódott (REST), és hogy B-nek olvasatlan üzenete jelenik meg a listán.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatFlowTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    private final RestTemplate rest = new RestTemplate();

    private String[] register(String email, String name) {
        String body = """
                {"email":"%s","displayName":"%s","password":"supersecret"}""".formatted(email, name);
        JsonNode node = postJson("/api/auth/register", body, null);
        return new String[]{node.get("accessToken").asText(), node.get("user").get("id").asText()};
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
        String resp = rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
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

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return client.connectAsync("ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<ChatMessageDto> subscribeMessages(StompSession session) {
        BlockingQueue<ChatMessageDto> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((ChatMessageDto) payload);
            }
        });
        return received;
    }

    @Test
    void twoUsersExchangeMessagesLive_andHistoryPersists() throws Exception {
        String[] a = register("chat-a@example.com", "Anna");
        String[] b = register("chat-b@example.com", "Bela");

        // A megnyitja a kétszemélyes szálat B-vel.
        JsonNode conv = postJson("/api/chat/conversations/direct",
                "{\"userId\":\"%s\"}".formatted(b[1]), a[0]);
        String conversationId = conv.get("id").asText();
        assertThat(conv.get("type").asText()).isEqualTo("DIRECT");
        assertThat(conv.get("title").asText()).isEqualTo("Bela");

        StompSession bSession = connect(b[0]);
        BlockingQueue<ChatMessageDto> bInbox = subscribeMessages(bSession);
        StompSession aSession = connect(a[0]);
        // A SUBSCRIBE keret aszinkron; rövid várakozás, hogy a szerver biztosan regisztrálja.
        Thread.sleep(400);

        aSession.send("/app/chat.send",
                new SendMessageRequest(UUID.fromString(conversationId), "Szia Bela!"));

        ChatMessageDto delivered = bInbox.poll(5, TimeUnit.SECONDS);
        assertThat(delivered).isNotNull();
        assertThat(delivered.content()).isEqualTo("Szia Bela!");
        assertThat(delivered.senderId()).isEqualTo(a[1]);
        assertThat(delivered.conversationId()).isEqualTo(conversationId);

        // Az előzmény perzisztált: B REST-en is visszakapja az üzenetet.
        JsonNode history = getJson("/api/chat/conversations/" + conversationId + "/messages", b[0]);
        JsonNode messages = history.get("messages");
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).get("content").asText()).isEqualTo("Szia Bela!");
    }

    @Test
    void conversationListShowsUnreadForRecipient() throws Exception {
        String[] a = register("chat-c@example.com", "Cili");
        String[] b = register("chat-d@example.com", "Dora");

        JsonNode conv = postJson("/api/chat/conversations/direct",
                "{\"userId\":\"%s\"}".formatted(b[1]), a[0]);
        String conversationId = conv.get("id").asText();

        // A küld egy üzenetet REST-en (megbízható tartalék-út).
        postJson("/api/chat/conversations/" + conversationId + "/messages",
                "{\"content\":\"Olvasatlan üzenet\"}", a[0]);

        JsonNode listForB = getJson("/api/chat/conversations", b[0]);
        assertThat(listForB).hasSize(1);
        JsonNode item = listForB.get(0);
        assertThat(item.get("id").asText()).isEqualTo(conversationId);
        assertThat(item.get("unreadCount").asLong()).isEqualTo(1);
        assertThat(item.get("lastMessagePreview").asText()).isEqualTo("Olvasatlan üzenet");

        // Miután B megnyitja (előzmény-lekérés), az olvasatlan nullázódik.
        getJson("/api/chat/conversations/" + conversationId + "/messages", b[0]);
        JsonNode listAfter = getJson("/api/chat/conversations", b[0]);
        assertThat(listAfter.get(0).get("unreadCount").asLong()).isEqualTo(0);
    }
}
