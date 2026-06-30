package com.nexa.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Egyszeri visszatöltés a hírfolyam-rendezéshez (#10): a folyam az utolsó aktivitáson rendez
 * ({@code Post.lastActivityAt}). A mező bevezetése előtt létrejött bejegyzéseknél ez {@code null},
 * így a korábbi hozzászólások aktivitása nem látszott a sorrendben (a kommentelt poszt nem került
 * felülre). Indításkor feltöltjük: a kommentelt posztoknál a legfrissebb hozzászólás idejére, a
 * többinél a létrehozás idejére.
 * <p>
 * A {@code null}-szűrő miatt idempotens: a már feltöltött vagy a fix után bump-olt sorokat nem
 * érinti, így a feltöltés utáni indításokon már nincs mit tennie.
 */
@Component
class PostActivityBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostActivityBackfill.class);

    private final PostRepository postRepository;

    PostActivityBackfill(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int fromComments = postRepository.backfillActivityFromComments();
        int fromCreated = postRepository.backfillActivityFromCreatedAt();
        if (fromComments + fromCreated > 0) {
            log.info("Hírfolyam-aktivitás visszatöltve: {} poszt a legfrissebb komment, {} a "
                    + "létrehozás ideje alapján", fromComments, fromCreated);
        }
    }
}
