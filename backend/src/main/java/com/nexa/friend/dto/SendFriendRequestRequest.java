package com.nexa.friend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Ismerőskérés küldésekor a címzett felhasználó azonosítója. */
public record SendFriendRequestRequest(@NotNull UUID userId) {
}
