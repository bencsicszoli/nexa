package com.nexa.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationReadRepository extends JpaRepository<ConversationRead, UUID> {

    Optional<ConversationRead> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    /** Egy felhasználó összes olvasottság-jelzője — a beszélgetéslista olvasatlan-számaihoz. */
    List<ConversationRead> findByUserId(UUID userId);
}
