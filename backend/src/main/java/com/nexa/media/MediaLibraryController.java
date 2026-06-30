package com.nexa.media;

import com.nexa.media.dto.MediaConfirmRequest;
import com.nexa.media.dto.MediaItemDto;
import com.nexa.media.dto.MediaUploadRequest;
import com.nexa.storage.PresignedUpload;
import com.nexa.subscription.SubscriptionRequired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Személyes médiatár-végpontok az {@code /api/library} prefix alatt — mind hitelesítést és
 * aktív előfizetést igényel. (A nyers fájlkiszolgálás külön, a {@code /api/media/**} útvonalon
 * megy — lásd {@code com.nexa.storage.MediaController} —, ezért külön a bázis.)
 */
@RestController
@RequestMapping("/api/library")
@SubscriptionRequired
public class MediaLibraryController {

    private final MediaService mediaService;

    public MediaLibraryController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /** Aláírt feltöltési cél egy médiatárba kerülő kép/videó számára. */
    @PostMapping("/upload-url")
    public PresignedUpload uploadUrl(@Valid @RequestBody MediaUploadRequest request) {
        return mediaService.createUpload(request.contentType());
    }

    /** Egy feltöltött elem megerősítése (perzisztálás a médiatárba). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaItemDto confirm(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody MediaConfirmRequest request) {
        return mediaService.confirm(userId, request.key(), request.type(), request.sizeBytes());
    }

    /** A bejelentkezett felhasználó médiatára (legfrissebb felül). */
    @GetMapping
    public List<MediaItemDto> list(@AuthenticationPrincipal UUID userId) {
        return mediaService.list(userId);
    }

    /** Egy saját médiatár-elem törlése (a fájl is törlődik). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        mediaService.delete(userId, id);
    }
}
