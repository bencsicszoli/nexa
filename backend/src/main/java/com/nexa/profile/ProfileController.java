package com.nexa.profile;

import com.nexa.auth.dto.UserDto;
import com.nexa.profile.dto.AvatarUploadRequest;
import com.nexa.profile.dto.ConfirmAvatarRequest;
import com.nexa.profile.dto.UpdateProfileRequest;
import com.nexa.storage.PresignedUpload;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Profil-végpontok az {@code /api/profile} prefix alatt — mind hitelesítést igényel.
 * Az avatar feltöltése presigned-URL mintára megy: link kérése → közvetlen feltöltés →
 * megerősítés a kulccsal.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserDto myProfile(@AuthenticationPrincipal UUID userId) {
        return profileService.getProfile(userId);
    }

    @PatchMapping
    public UserDto update(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(userId, request.displayName(), request.bio());
    }

    @PostMapping("/avatar/upload-url")
    public PresignedUpload avatarUploadUrl(@Valid @RequestBody AvatarUploadRequest request) {
        return profileService.createAvatarUpload(request.contentType());
    }

    @PutMapping("/avatar")
    public UserDto confirmAvatar(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ConfirmAvatarRequest request) {
        return profileService.confirmAvatar(userId, request.key());
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.OK)
    public UserDto removeAvatar(@AuthenticationPrincipal UUID userId) {
        return profileService.removeAvatar(userId);
    }
}
