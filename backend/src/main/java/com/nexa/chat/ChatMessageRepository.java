package com.nexa.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Egy szál legfrissebb üzenetei (idő szerint csökkenő, holtversenyben id szerint). */
    @Query("""
            select m from ChatMessage m
            where m.conversation.id = :conversationId
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findLatest(@Param("conversationId") UUID conversationId, Pageable pageable);

    /**
     * A megadott (createdAt, id) kurzornál régebbi üzenetek — a „régebbi üzenetek betöltése"
     * lapozáshoz. A holtverseny-feloldás az id-vel a kurzor-alapú hírfolyaméval (#10) egyezik.
     */
    @Query("""
            select m from ChatMessage m
            where m.conversation.id = :conversationId
              and (m.createdAt < :ts or (m.createdAt = :ts and m.id < :id))
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findBefore(@Param("conversationId") UUID conversationId,
                                 @Param("ts") Instant ts,
                                 @Param("id") UUID id,
                                 Pageable pageable);

    /**
     * Olvasatlan üzenetek száma egy szálban a hívó szemszögéből: a {@code lastReadAt} utáni,
     * nem a hívótól származó üzenetek. {@code lastReadAt == null} esetén minden idegen üzenet
     * olvasatlan.
     */
    @Query("""
            select count(m) from ChatMessage m
            where m.conversation.id = :conversationId
              and m.sender.id <> :userId
              and (:lastReadAt is null or m.createdAt > :lastReadAt)
            """)
    long countUnread(@Param("conversationId") UUID conversationId,
                     @Param("userId") UUID userId,
                     @Param("lastReadAt") Instant lastReadAt);
}
