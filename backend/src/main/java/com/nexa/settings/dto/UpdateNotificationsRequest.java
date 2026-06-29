package com.nexa.settings.dto;

import com.nexa.user.NotificationPrefs;

/** Az értesítési preferenciák mentése típusonként (#17). */
public record UpdateNotificationsRequest(
        boolean newPost,
        boolean friendRequest,
        boolean friendAccepted,
        boolean newFollower) {

    public NotificationPrefs toPrefs() {
        return new NotificationPrefs(newPost, friendRequest, friendAccepted, newFollower);
    }
}
