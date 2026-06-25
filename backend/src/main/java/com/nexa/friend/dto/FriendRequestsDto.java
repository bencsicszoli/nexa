package com.nexa.friend.dto;

import java.util.List;

/**
 * A függőben lévő ismerőskérések két iránya egy válaszban: a felhasználóhoz beérkezett
 * ({@code incoming}, ezeket fogadhatja el / utasíthatja el) és az általa küldött
 * ({@code outgoing}, ezeket visszavonhatja) kérések.
 */
public record FriendRequestsDto(
        List<FriendRequestDto> incoming,
        List<FriendRequestDto> outgoing) {
}
