package com.nexa.realtime;

import com.nexa.realtime.dto.NotificationPageDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Értesítés-végpontok az {@code /api/notifications} prefix alatt — hitelesítést igényelnek, de
 * <b>NEM</b> előfizetés-gateltek (#17): az értesítések olvasása a lejárt trialú felhasználónak is
 * elérhető marad (a paywall mögött csak az aktív funkciók vannak). Lapozott előzmény, olvasatlan-szám
 * és olvasott-jelölés.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Az értesítés-előzmény egy lapja, legfrissebb felül. */
    @GetMapping
    public NotificationPageDto history(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return notificationService.history(userId, page, size);
    }

    /** Az olvasatlan értesítések száma a harang-jelvényhez. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UUID userId) {
        return Map.of("count", notificationService.unreadCount(userId));
    }

    /** Az összes értesítés olvasottra állítása. */
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal UUID userId) {
        notificationService.markAllRead(userId);
    }

    /** Egy értesítés olvasottra állítása (csak a sajátját). */
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        notificationService.markRead(userId, id);
    }
}
