package com.nexa.chat.dto;

/**
 * Gépelés-jelzés a szál többi résztvevőjének (kimenő, {@code /user/queue/chat.typing}).
 * A {@code userName} a csoport-szálban a „X gépel…" felirathoz kell.
 */
public record TypingNotice(String conversationId, String userId, String userName) {
}
