package com.nexa.settings;

import com.nexa.settings.dto.ChangePasswordRequest;
import com.nexa.settings.dto.SettingsDto;
import com.nexa.settings.dto.UpdateLocaleRequest;
import com.nexa.settings.dto.UpdateNotificationsRequest;
import com.nexa.settings.dto.UpdatePrivacyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Beállítások-végpontok az {@code /api/settings} prefix alatt (#17) — hitelesítést igényelnek, de
 * <b>NEM</b> előfizetés-gateltek (a saját fiók kezelése a paywall mögött is elérhető). Nyelv,
 * értesítési preferenciák, adatvédelmi kapcsolók és jelszóváltás.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** A beállítások teljes állapota. */
    @GetMapping
    public SettingsDto get(@AuthenticationPrincipal UUID userId) {
        return settingsService.get(userId);
    }

    /** A felület nyelvének mentése (HU/EN). */
    @PatchMapping("/locale")
    public SettingsDto updateLocale(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateLocaleRequest request) {
        return settingsService.updateLocale(userId, request.locale());
    }

    /** Az értesítési preferenciák mentése. */
    @PatchMapping("/notifications")
    public SettingsDto updateNotifications(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateNotificationsRequest request) {
        return settingsService.updateNotifications(userId, request.toPrefs());
    }

    /** Az adatvédelmi kapcsolók mentése (kereshetőség, jelenlét elrejtése). */
    @PatchMapping("/privacy")
    public SettingsDto updatePrivacy(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdatePrivacyRequest request) {
        return settingsService.updatePrivacy(userId, request.searchable(), request.hidePresence());
    }

    /** Jelszóváltás (siker után az összes munkamenet megszakad — a kliens kiléptet). */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        settingsService.changePassword(userId, request.currentPassword(), request.newPassword());
    }
}
