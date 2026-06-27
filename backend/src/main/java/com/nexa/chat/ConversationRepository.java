package com.nexa.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * A két felhasználó közötti kétszemélyes szál (ha létezik). A DIRECT szálnak pontosan két
     * résztvevője van, ezért az „mindkettő résztvevő" feltétel egyértelműen azonosítja —
     * így nem jön létre duplikált 1:1 beszélgetés.
     */
    @Query("""
            select c from Conversation c
            where c.type = com.nexa.chat.ConversationType.DIRECT
              and exists (select 1 from ConversationParticipant p
                          where p.conversation = c and p.user.id = :a)
              and exists (select 1 from ConversationParticipant p
                          where p.conversation = c and p.user.id = :b)
            """)
    Optional<Conversation> findDirectBetween(@Param("a") UUID a, @Param("b") UUID b);

    /** Egy csoport csevegő-szála (csoportonként legfeljebb egy). */
    Optional<Conversation> findByGroupId(UUID groupId);

    /** A felhasználó kétszemélyes szálai, legutóbb aktív felül. */
    @Query("""
            select c from Conversation c
            where c.type = com.nexa.chat.ConversationType.DIRECT
              and exists (select 1 from ConversationParticipant p
                          where p.conversation = c and p.user.id = :userId)
            order by c.lastMessageAt desc
            """)
    List<Conversation> findDirectForUser(@Param("userId") UUID userId);

    /** A megadott csoportok csevegő-szálai (a felhasználó tag-csoportjaihoz), legutóbb aktív felül. */
    @Query("""
            select c from Conversation c
            where c.type = com.nexa.chat.ConversationType.GROUP
              and c.group.id in :groupIds
            order by c.lastMessageAt desc
            """)
    List<Conversation> findGroupConversations(@Param("groupIds") List<UUID> groupIds);
}
