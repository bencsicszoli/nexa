package com.nexa.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    /** Egy DIRECT szál résztvevőinek felhasználó-id-ja — a fan-out címzettjeihez. */
    @Query("""
            select p.user.id from ConversationParticipant p
            where p.conversation.id = :conversationId
            """)
    List<UUID> findUserIdsByConversationId(@Param("conversationId") UUID conversationId);
}
