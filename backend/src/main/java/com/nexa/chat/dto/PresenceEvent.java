package com.nexa.chat.dto;

/** Online/offline állapotváltás egy felhasználóról (kimenő, {@code /topic/presence}). */
public record PresenceEvent(String userId, boolean online) {
}
