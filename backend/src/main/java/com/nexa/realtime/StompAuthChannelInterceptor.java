package com.nexa.realtime;

import com.nexa.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A STOMP CONNECT keret hitelesítése. A böngésző a WebSocket-kézfogásnál nem tud
 * tetszőleges fejlécet küldeni, ezért a JWT a STOMP CONNECT natív {@code Authorization}
 * fejlécében érkezik (ugyanaz a {@code Bearer <token>} formátum, mint a REST-nél).
 * Érvényes token esetén a munkamenet {@link StompPrincipal}-t kap (name = userId), így
 * a {@code convertAndSendToUser} a megfelelő klienshez talál; érvénytelen/hiányzó token
 * esetén a CONNECT elutasításra kerül.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Hiányzó vagy hibás Authorization fejléc a STOMP CONNECT-en.");
        }
        try {
            UUID userId = jwtService.extractUserId(header.substring(7));
            accessor.setUser(new StompPrincipal(userId.toString()));
        } catch (Exception e) {
            throw new MessagingException("Érvénytelen vagy lejárt token a STOMP CONNECT-en.");
        }
        return message;
    }
}
