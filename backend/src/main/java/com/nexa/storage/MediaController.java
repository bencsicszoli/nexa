package com.nexa.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

/**
 * A lemezre mentett objektumok publikus kiszolgálása ({@code GET /api/media/<key>}).
 * Csak a {@code local} providernél él; éles R2-nél a böngésző közvetlenül a tárolóból
 * tölti a képet, így ott erre nincs szükség.
 */
@RestController
@RequestMapping("/api/media")
@ConditionalOnProperty(name = "nexa.storage.provider", havingValue = "local", matchIfMissing = true)
public class MediaController {

    private final LocalStorageService storage;

    public MediaController(LocalStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/{*key}")
    public ResponseEntity<Resource> get(@PathVariable("key") String key) {
        // A vezető '/'-t a {*key} bevezeti — vágjuk le a kulcs feloldása előtt.
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        Path file = storage.resolveForRead(normalized);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storage.contentTypeOf(file)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(new PathResource(file));
    }
}
