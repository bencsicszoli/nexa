package com.nexa.realtime.dto;

import java.util.List;

/**
 * Az értesítés-előzmény egy lapja (#17): a tételek, az aktuális lapszám és hogy van-e még
 * régebbi lap. A kliens a {@code hasMore}-ral dönt a „Továbbiak betöltése" gomb megjelenítéséről.
 */
public record NotificationPageDto(
        List<NotificationDto> items,
        int page,
        boolean hasMore) {
}
