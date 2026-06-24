package com.nexa.post.dto;

import jakarta.validation.constraints.Size;

/**
 * Bejegyzés szerkesztésekor küldött adat. Csak a szöveg módosítható (a média
 * változatlan); üres is lehet, ha a poszton van média (ezt a {@code PostService} ellenőrzi).
 */
public record UpdatePostRequest(@Size(max = 5000) String content) {
}
