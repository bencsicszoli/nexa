package com.nexa.settings.dto;

import com.nexa.user.NotificationPrefs;
import com.nexa.user.User;

/**
 * A beállítások oldal teljes állapota (#17): nyelv, adatvédelmi kapcsolók, értesítési
 * preferenciák és a 2FA állapota. A jelszót/TOTP-titkot természetesen nem adjuk vissza.
 */
public record SettingsDto(
        String locale,
        boolean searchable,
        boolean hidePresence,
        NotificationPrefs notificationPrefs,
        boolean totpEnabled) {

    public static SettingsDto from(User user) {
        return new SettingsDto(
                user.getLocale(),
                user.isSearchable(),
                user.isHidePresence(),
                user.getNotificationPrefs(),
                user.isTotpEnabled());
    }
}
