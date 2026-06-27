package com.nexa.chat.dto;

import com.nexa.chat.ChatMessage;

import java.time.Instant;

/**
 * Egy csevegő-üzenet a kliensnek. A küldő nevét/avatárját is hordozza, hogy a csoport-szálban
 * a buborékhoz ne kelljen külön felhasználó-lekérdezés.
 */
public record ChatMessageDto(
        String id,
        String conversationId,
        String senderId,
        String senderName,
        String senderAvatarUrl,
        String content,
        Instant createdAt) {

    public static ChatMessageDto of(ChatMessage message) {
        return new ChatMessageDto(
                message.getId().toString(),
                message.getConversation().getId().toString(),
                message.getSender().getId().toString(),
                message.getSender().getDisplayName(),
                message.getSender().getAvatarUrl(),
                message.getContent(),
                message.getCreatedAt());
    }
}
