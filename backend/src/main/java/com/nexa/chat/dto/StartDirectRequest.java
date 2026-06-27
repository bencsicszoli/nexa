package com.nexa.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Kétszemélyes beszélgetés megnyitása/létrehozása egy másik felhasználóval. */
public record StartDirectRequest(@NotNull UUID userId) {
}
