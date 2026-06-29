package com.nexa.settings;

import com.nexa.auth.AuthService;
import com.nexa.common.ApiException;
import com.nexa.settings.dto.SettingsDto;
import com.nexa.user.NotificationPrefs;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A felhasználói beállítások üzleti logikája (#17): nyelv, értesítési preferenciák, adatvédelmi
 * kapcsolók és jelszóváltás. A 2FA-t a {@link com.nexa.auth.TwoFactorService} kezeli, a jelszóváltást
 * az {@link AuthService} (a refresh tokenek visszavonásával együtt).
 */
@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public SettingsService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public SettingsDto get(UUID userId) {
        return SettingsDto.from(load(userId));
    }

    @Transactional
    public SettingsDto updateLocale(UUID userId, String locale) {
        User user = load(userId);
        user.setLocale(locale);
        return SettingsDto.from(user);
    }

    @Transactional
    public SettingsDto updateNotifications(UUID userId, NotificationPrefs prefs) {
        User user = load(userId);
        user.setNotificationPrefs(prefs);
        return SettingsDto.from(user);
    }

    @Transactional
    public SettingsDto updatePrivacy(UUID userId, boolean searchable, boolean hidePresence) {
        User user = load(userId);
        user.setSearchable(searchable);
        user.setHidePresence(hidePresence);
        return SettingsDto.from(user);
    }

    /** Jelszóváltás — az {@link AuthService} ellenőriz, hash-el és visszavonja a refresh tokeneket. */
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        authService.changePassword(userId, currentPassword, newPassword);
    }

    private User load(UUID userId) {
        return userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
    }
}
