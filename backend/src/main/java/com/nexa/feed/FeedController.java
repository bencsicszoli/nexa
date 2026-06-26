package com.nexa.feed;

import com.nexa.feed.dto.FeedPageDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A hírfolyam végpontja (#10) az {@code /api/feed} alatt — hitelesítést igényel.
 * Időrendi, algoritmusmentes folyam a hívó ismerőseitől, követettjeitől és tag-csoportjaitól.
 * A lapozás cursor-alapú: a válasz {@code nextCursor}-át kell visszaküldeni a következő lapért.
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * A hírfolyam egy lapja. {@code cursor} üres/hiányzó az első lapnál; {@code limit} a
     * lapméret (alap {@value FeedService#DEFAULT_LIMIT}, felső korlát {@value FeedService#MAX_LIMIT}).
     */
    @GetMapping
    public FeedPageDto feed(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "" + FeedService.DEFAULT_LIMIT) int limit) {
        return feedService.getFeed(userId, cursor, limit);
    }
}
