package com.nexa.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP konfiguráció a valós idejű értesítéshez (#11).
 * <ul>
 *   <li>A kliens a {@code /ws} végponton csatlakozik (natív WebSocket, SockJS nélkül).</li>
 *   <li>A szerver-oldali, beépített üzenetbróker (in-memory) a {@code /topic} és {@code /queue}
 *       célokra szór szét; a felhasználó-specifikus értesítés a {@code /user/queue/...} úton megy.</li>
 *   <li>A CONNECT keret JWT-hitelesítése a {@link StompAuthChannelInterceptor}-ban történik.</li>
 * </ul>
 *
 * <p><b>Skálázás:</b> egy backend-példánynál az in-memory bróker elég a fan-outhoz. Több
 * példány esetén a {@code /topic}-fan-out Redis pub/subbal terjeszthető ki példányok között
 * (STOMP relay) — ezt csak a mérés indokolja, ezért most nem vezetjük be (lásd CLAUDE.md).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            StompAuthChannelInterceptor authInterceptor,
            @Value("${nexa.cors.allowed-origins}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
