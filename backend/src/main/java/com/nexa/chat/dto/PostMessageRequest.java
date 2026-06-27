package com.nexa.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Üzenetküldés törzse a REST tartalék-végponthoz (a szál id-ja az útvonalban van). */
public record PostMessageRequest(@NotBlank @Size(max = 4000) String content) {
}
