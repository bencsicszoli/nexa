package com.nexa.search.dto;

import com.nexa.group.dto.GroupDto;
import com.nexa.post.dto.PostDto;

import java.util.List;

/**
 * A keresés (#16) aggregált találatai egy válaszban, mindhárom típusra: felhasználók, csoportok
 * és bejegyzések. A frontend a fülekkel (Mind / Emberek / Csoportok / Bejegyzések) szűr a kapott
 * listák között — egyetlen hívásból. Minden lista típusonként korlátozott (lásd {@code SearchService}).
 */
public record SearchResultsDto(
        List<SearchUserDto> users,
        List<GroupDto> groups,
        List<PostDto> posts) {

    /** Üres találat (üres/rövid keresőkifejezésre) — nincs külön null-kezelés a frontenden. */
    public static SearchResultsDto empty() {
        return new SearchResultsDto(List.of(), List.of(), List.of());
    }
}
