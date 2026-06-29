package com.nexa.search;

import com.nexa.search.dto.SearchResultsDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Keresés (#16) az {@code /api/search} prefix alatt — hitelesítést igényel. A böngésző/olvasó
 * végpontokhoz hasonlóan (felhasználói profil, csoport-böngészés) <b>nincs</b> előfizetés-gating:
 * a tartalom-felfedezés a fizetés nélküli (lejárt trial) felhasználónak is elérhető marad, az
 * írás/aktív funkciók viszont a paywall mögött vannak.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** Egyesített keresés felhasználókra, csoportokra és bejegyzésekre egy kifejezésre. */
    @GetMapping
    public SearchResultsDto search(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(name = "q", required = false) String query) {
        return searchService.search(userId, query);
    }
}
