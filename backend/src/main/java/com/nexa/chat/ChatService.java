package com.nexa.chat;

import com.nexa.chat.dto.ChatMessageDto;
import com.nexa.chat.dto.ConversationDto;
import com.nexa.chat.dto.MessagesPageDto;
import com.nexa.chat.dto.TypingNotice;
import com.nexa.common.ApiException;
import com.nexa.group.Group;
import com.nexa.group.GroupMemberRepository;
import com.nexa.group.GroupRepository;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A csevegés (#12) üzleti logikája: kétszemélyes és csoport-szálak megnyitása, a
 * beszélgetéslista, az időrendi üzenet-előzmény (cursor-alapú lapozás), üzenetküldés a
 * fan-out push-sal, az olvasottság és a gépelés-jelzés.
 *
 * <p>Az új üzenet a DB-tranzakció <b>commitja után</b> kerül kiküldésre minden résztvevőnek
 * (a küldőt is beleértve, hogy a saját másik fülei is frissüljenek) — ugyanaz a minta, mint
 * a valós idejű értesítésnél (#11): rollbacknál nem szivárog ki nemlétező üzenet, kattintásra
 * pedig az előzmény-lekérdezés már látja a perzisztált üzenetet.
 */
@Service
public class ChatService {

    /** Az üzenetlap alapmérete és felső korlátja. */
    public static final int DEFAULT_LIMIT = 30;
    public static final int MAX_LIMIT = 50;

    private static final String MESSAGES_DEST = "/queue/messages";
    private static final String TYPING_DEST = "/queue/chat.typing";

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final ConversationReadRepository readRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messaging;

    public ChatService(ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       ChatMessageRepository messageRepository,
                       ConversationReadRepository readRepository,
                       UserRepository userRepository,
                       GroupRepository groupRepository,
                       GroupMemberRepository groupMemberRepository,
                       PresenceService presenceService,
                       SimpMessagingTemplate messaging) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.readRepository = readRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.presenceService = presenceService;
        this.messaging = messaging;
    }

    // --- Szál megnyitása ---

    /** Kétszemélyes szál megnyitása/megtalálása egy másik felhasználóval (idempotens). */
    @Transactional
    public ConversationDto startDirect(UUID userId, UUID otherId) {
        if (userId.equals(otherId)) {
            throw ApiException.selfConversation();
        }
        User other = userRepository.findById(otherId).orElseThrow(ApiException::userNotFound);
        User self = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);

        Conversation conversation = conversationRepository.findDirectBetween(userId, otherId)
                .orElseGet(() -> {
                    Conversation created = conversationRepository.save(Conversation.direct());
                    participantRepository.save(new ConversationParticipant(created, self));
                    participantRepository.save(new ConversationParticipant(created, other));
                    return created;
                });
        return toDto(conversation, userId, readMap(userId));
    }

    /** Egy csoport csevegő-szálának megnyitása/létrehozása (csak tagnak). */
    @Transactional
    public ConversationDto openGroup(UUID userId, UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw ApiException.notGroupMember();
        }
        Conversation conversation = conversationRepository.findByGroupId(groupId)
                .orElseGet(() -> conversationRepository.save(Conversation.forGroup(group)));
        return toDto(conversation, userId, readMap(userId));
    }

    // --- Lista ---

    /** A felhasználó összes beszélgetése, legutóbb aktív felül. */
    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(UUID userId) {
        List<Conversation> conversations = new ArrayList<>(conversationRepository.findDirectForUser(userId));
        List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        if (!groupIds.isEmpty()) {
            conversations.addAll(conversationRepository.findGroupConversations(groupIds));
        }
        conversations.sort(Comparator.comparing(Conversation::getLastMessageAt).reversed());

        Map<UUID, Instant> reads = readMap(userId);
        return conversations.stream().map(c -> toDto(c, userId, reads)).toList();
    }

    // --- Előzmény ---

    /**
     * Egy szál üzenet-előzményének egy lapja, időrendben (legrégebbi elöl). A lekérés egyúttal
     * olvasottá teszi a szálat a hívónak. {@code cursor} null az első (legfrissebb) lapnál.
     */
    @Transactional
    public MessagesPageDto messages(UUID userId, UUID conversationId, String cursor, int limit) {
        Conversation conversation = requireAccessible(conversationId, userId);
        int pageSize = Math.max(1, Math.min(limit, MAX_LIMIT));
        Pageable page = PageRequest.of(0, pageSize + 1);

        List<ChatMessage> newestFirst;
        if (cursor == null || cursor.isBlank()) {
            newestFirst = messageRepository.findLatest(conversationId, page);
        } else {
            Cursor c = decodeCursor(cursor);
            newestFirst = messageRepository.findBefore(conversationId, c.createdAt(), c.id(), page);
        }

        boolean hasMore = newestFirst.size() > pageSize;
        List<ChatMessage> pageMessages = hasMore ? newestFirst.subList(0, pageSize) : newestFirst;
        String nextCursor = hasMore && !pageMessages.isEmpty()
                ? encodeCursor(pageMessages.get(pageMessages.size() - 1)) : null;

        // A kliensnek időrendben (legrégebbi elöl) adjuk vissza, hogy egyből megjeleníthesse.
        List<ChatMessageDto> ordered = new ArrayList<>(pageMessages.size());
        for (int i = pageMessages.size() - 1; i >= 0; i--) {
            ordered.add(ChatMessageDto.of(pageMessages.get(i)));
        }

        markReadInternal(userId, conversationId, Instant.now());
        return new MessagesPageDto(toDto(conversation, userId, readMap(userId)), ordered, nextCursor);
    }

    // --- Küldés ---

    /** Üzenet küldése egy szálba: perzisztálás + fan-out push a résztvevőknek. */
    @Transactional
    public ChatMessageDto sendMessage(UUID senderId, UUID conversationId, String content) {
        Conversation conversation = requireAccessible(conversationId, senderId);
        String trimmed = content == null ? "" : content.strip();
        if (trimmed.isEmpty()) {
            throw ApiException.emptyMessage();
        }
        User sender = userRepository.findById(senderId).orElseThrow(ApiException::userNotFound);

        ChatMessage message = messageRepository.save(new ChatMessage(conversation, sender, trimmed));
        conversation.recordMessage(message.getCreatedAt(), trimmed);

        ChatMessageDto dto = ChatMessageDto.of(message);
        Set<UUID> recipients = recipients(conversation);
        publishAfterCommit(recipients, dto);
        return dto;
    }

    // --- Olvasottság ---

    @Transactional
    public void markRead(UUID userId, UUID conversationId) {
        requireAccessible(conversationId, userId);
        markReadInternal(userId, conversationId, Instant.now());
    }

    // --- Gépelés ---

    /** Gépelés-jelzés továbbítása a szál többi résztvevőjének (a küldő kihagyásával). */
    @Transactional(readOnly = true)
    public void broadcastTyping(UUID userId, UUID conversationId) {
        Conversation conversation = requireAccessible(conversationId, userId);
        User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        TypingNotice notice = new TypingNotice(
                conversationId.toString(), userId.toString(), user.getDisplayName());
        for (UUID recipient : recipients(conversation)) {
            if (!recipient.equals(userId)) {
                messaging.convertAndSendToUser(recipient.toString(), TYPING_DEST, notice);
            }
        }
    }

    // --- Belső segédek ---

    /** Betölti a szálat, és ellenőrzi, hogy a hívó hozzáfér-e (különben 404 — létezést sem szivárogtatunk). */
    private Conversation requireAccessible(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(ApiException::conversationNotFound);
        if (!hasAccess(conversation, userId)) {
            throw ApiException.conversationNotFound();
        }
        return conversation;
    }

    private boolean hasAccess(Conversation conversation, UUID userId) {
        if (conversation.getType() == ConversationType.DIRECT) {
            return participantRepository.existsByConversationIdAndUserId(conversation.getId(), userId);
        }
        return groupMemberRepository.existsByGroupIdAndUserId(conversation.getGroup().getId(), userId);
    }

    /** A szál push-címzettjei (a küldőt is beleértve, hogy a saját fülei is frissüljenek). */
    Set<UUID> recipients(Conversation conversation) {
        if (conversation.getType() == ConversationType.DIRECT) {
            return new java.util.LinkedHashSet<>(
                    participantRepository.findUserIdsByConversationId(conversation.getId()));
        }
        return new java.util.LinkedHashSet<>(
                groupMemberRepository.findUserIdsByGroupId(conversation.getGroup().getId()));
    }

    private void markReadInternal(UUID userId, UUID conversationId, Instant at) {
        readRepository.findByConversationIdAndUserId(conversationId, userId)
                .ifPresentOrElse(
                        r -> r.setLastReadAt(at),
                        () -> readRepository.save(new ConversationRead(conversationId, userId, at)));
    }

    private Map<UUID, Instant> readMap(UUID userId) {
        Map<UUID, Instant> map = new HashMap<>();
        for (ConversationRead r : readRepository.findByUserId(userId)) {
            map.put(r.getConversationId(), r.getLastReadAt());
        }
        return map;
    }

    private ConversationDto toDto(Conversation c, UUID viewerId, Map<UUID, Instant> reads) {
        // Olvasottság-rekord híján EPOCH a sentinel (minden idegen üzenet olvasatlan); a null
        // paramétert a PostgreSQL nem tudná típushoz kötni a countUnread-ben.
        Instant lastReadAt = reads.getOrDefault(c.getId(), Instant.EPOCH);
        long unread = messageRepository.countUnread(c.getId(), viewerId, lastReadAt);

        String title;
        String imageUrl;
        String otherUserId = null;
        String groupId = null;
        boolean online = false;

        if (c.getType() == ConversationType.DIRECT) {
            User other = participantRepository.findByConversationId(c.getId()).stream()
                    .map(ConversationParticipant::getUser)
                    .filter(u -> !u.getId().equals(viewerId))
                    .findFirst()
                    .orElseThrow(ApiException::conversationNotFound);
            title = other.getDisplayName();
            imageUrl = other.getAvatarUrl();
            otherUserId = other.getId().toString();
            online = presenceService.isOnline(other.getId());
        } else {
            Group group = c.getGroup();
            title = group.getName();
            imageUrl = group.getLogoUrl();
            groupId = group.getId().toString();
        }

        return new ConversationDto(
                c.getId().toString(),
                c.getType().name(),
                title,
                imageUrl,
                otherUserId,
                groupId,
                online,
                c.getLastMessagePreview(),
                c.getLastMessageAt(),
                unread);
    }

    private void publishAfterCommit(Set<UUID> recipients, ChatMessageDto dto) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(recipients, dto);
                }
            });
        } else {
            send(recipients, dto);
        }
    }

    private void send(Set<UUID> recipients, ChatMessageDto dto) {
        for (UUID recipient : recipients) {
            messaging.convertAndSendToUser(recipient.toString(), MESSAGES_DEST, dto);
        }
    }

    // --- Cursor (a hírfolyaméval (#10) azonos minta) ---

    private record Cursor(Instant createdAt, UUID id) {
    }

    private String encodeCursor(ChatMessage message) {
        String raw = message.getCreatedAt().toString() + "|" + message.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf('|');
            if (sep < 0) {
                throw new IllegalArgumentException("missing separator");
            }
            Instant createdAt = Instant.parse(raw.substring(0, sep));
            UUID id = UUID.fromString(raw.substring(sep + 1));
            return new Cursor(createdAt, id);
        } catch (RuntimeException e) {
            throw ApiException.invalidCursor();
        }
    }
}
