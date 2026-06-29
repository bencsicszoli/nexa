package com.nexa.settings.dto;

import jakarta.validation.constraints.Pattern;

/** A felület nyelvének mentése (#17). Csak a támogatott nyelvek: magyar / angol. */
public record UpdateLocaleRequest(
        @Pattern(regexp = "hu|en", message = "Supported locales: hu, en") String locale) {
}
