package com.nexa.media;

import com.nexa.common.ApiException;
import com.nexa.media.dto.MediaItemDto;
import com.nexa.storage.DeferredStorageDeleter;
import com.nexa.storage.PresignedUpload;
import com.nexa.storage.StorageService;
import com.nexa.post.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A személyes médiatár üzleti logikája: közvetlen (poszttól független) kép/videó-feltöltés,
 * listázás és törlés. A média presigned URL-re kerül fel (lásd {@link StorageService}), a
 * {@code library/} prefix alá; a DB csak a publikus URL-re hivatkozik. A fájltörlés a DB-commit
 * után, best-effort fut ({@link DeferredStorageDeleter}), így rollbacknél nem törlünk élő fájlt.
 */
@Service
public class MediaService {

    /** A médiatár logikai mappája a tárolóban. */
    private static final String MEDIA_PREFIX = "library";
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_VIDEO_TYPES =
            Set.of("video/mp4", "video/webm", "video/x-matroska");

    private final MediaItemRepository mediaItemRepository;
    private final StorageService storageService;
    private final DeferredStorageDeleter storageDeleter;

    public MediaService(MediaItemRepository mediaItemRepository, StorageService storageService,
                        DeferredStorageDeleter storageDeleter) {
        this.mediaItemRepository = mediaItemRepository;
        this.storageService = storageService;
        this.storageDeleter = storageDeleter;
    }

    /** Aláírt feltöltési cél; csak engedélyezett kép/videó típust enged. */
    public PresignedUpload createUpload(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalized) && !ALLOWED_VIDEO_TYPES.contains(normalized)) {
            throw ApiException.unsupportedMediaType();
        }
        return storageService.createUpload(MEDIA_PREFIX, normalized);
    }

    /**
     * Egy feltöltött médiatár-elem megerősítése (perzisztálás). Csak a médiatár-mappába
     * ({@value #MEDIA_PREFIX}/) mutató kulcsot fogad el.
     */
    @Transactional
    public MediaItemDto confirm(UUID ownerId, String key, MediaType type, long sizeBytes) {
        if (key == null || !key.startsWith(MEDIA_PREFIX + "/")) {
            throw ApiException.invalidUpload();
        }
        MediaItem item = mediaItemRepository.save(
                new MediaItem(ownerId, storageService.publicUrl(key), type, sizeBytes));
        return MediaItemDto.from(item);
    }

    /** A felhasználó médiatára időrendben (legfrissebb felül). */
    @Transactional(readOnly = true)
    public List<MediaItemDto> list(UUID ownerId) {
        return mediaItemRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(MediaItemDto::from)
                .toList();
    }

    /**
     * Egy médiatár-elem törlése — csak a tulajdonos teheti (idegen/nem létező → 404, a létezést
     * sem szivárogtatjuk). A fájl a commit után törlődik; a korábban posztba megosztott (külön
     * feltöltött) másolatokat nem érinti.
     */
    @Transactional
    public void delete(UUID ownerId, UUID id) {
        MediaItem item = mediaItemRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(ApiException::mediaNotFound);
        String key = storageService.keyFromPublicUrl(item.getUrl());
        mediaItemRepository.delete(item);
        storageDeleter.deleteAfterCommit(List.of(key == null ? "" : key));
    }
}
