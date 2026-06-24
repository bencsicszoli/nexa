package com.nexa.profile;

import com.nexa.auth.dto.UserDto;
import com.nexa.common.ApiException;
import com.nexa.storage.PresignedUpload;
import com.nexa.storage.StorageService;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * A profil üzleti logikája: megtekintés, szerkesztés (név + bio) és avatar-kezelés
 * presigned feltöltéssel. A kép maga az objektumtárolóba kerül, a DB csak az URL-t tárolja.
 */
@Service
public class ProfileService {

    /** Az avatar kulcsainak logikai mappája a tárolóban. */
    private static final String AVATAR_PREFIX = "avatars";
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final UserRepository userRepository;
    private final StorageService storageService;

    public ProfileService(UserRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return UserDto.from(loadUser(userId));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String displayName, String bio) {
        User user = loadUser(userId);
        user.setDisplayName(displayName.trim());
        String trimmedBio = bio == null ? null : bio.trim();
        user.setBio(trimmedBio == null || trimmedBio.isEmpty() ? null : trimmedBio);
        return UserDto.from(user);
    }

    /** Aláírt avatar-feltöltési cél; csak képtípust enged. */
    public PresignedUpload createAvatarUpload(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalized)) {
            throw ApiException.unsupportedImageType();
        }
        return storageService.createUpload(AVATAR_PREFIX, normalized);
    }

    @Transactional
    public UserDto confirmAvatar(UUID userId, String key) {
        // Csak a saját avatar-mappába mutató kulcsot fogadunk el (nem tetszőleges objektum).
        if (key == null || !key.startsWith(AVATAR_PREFIX + "/")) {
            throw ApiException.invalidUpload();
        }
        User user = loadUser(userId);
        user.setAvatarUrl(storageService.publicUrl(key));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto removeAvatar(UUID userId) {
        User user = loadUser(userId);
        user.setAvatarUrl(null);
        return UserDto.from(user);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
    }
}
