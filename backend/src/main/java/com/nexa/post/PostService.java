package com.nexa.post;

import com.nexa.common.ApiException;
import com.nexa.group.Group;
import com.nexa.post.dto.CreatePostRequest;
import com.nexa.post.dto.MediaItem;
import com.nexa.post.dto.PostDto;
import com.nexa.storage.DeferredStorageDeleter;
import com.nexa.storage.PresignedUpload;
import com.nexa.storage.StorageService;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bejegyzések üzleti logikája: létrehozás (szöveg és/vagy kép/videó) és a saját
 * profilon való listázás. A média presigned URL-re kerül fel (lásd {@link StorageService}),
 * a poszt csak a tárolóbeli kulcsra hivatkozik. A hírfolyam-aggregáció (#10) külön kártya.
 */
@Service
public class PostService {

    /** A poszt-médiák logikai mappája a tárolóban. */
    private static final String MEDIA_PREFIX = "posts";
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_VIDEO_TYPES =
            Set.of("video/mp4", "video/webm", "video/x-matroska");

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final DeferredStorageDeleter storageDeleter;

    public PostService(PostRepository postRepository, UserRepository userRepository,
                       StorageService storageService, DeferredStorageDeleter storageDeleter) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.storageDeleter = storageDeleter;
    }

    /** Aláírt média-feltöltési cél; csak engedélyezett kép/videó típust enged. */
    public PresignedUpload createMediaUpload(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalized) && !ALLOWED_VIDEO_TYPES.contains(normalized)) {
            throw ApiException.unsupportedMediaType();
        }
        return storageService.createUpload(MEDIA_PREFIX, normalized);
    }

    /** Profil-bejegyzés létrehozása (nincs csoport). */
    @Transactional
    public PostDto create(UUID authorId, CreatePostRequest request) {
        return create(authorId, request, null);
    }

    /**
     * Bejegyzés létrehozása. {@code group} nullnál profil-poszt, egyébként a csoportba
     * kerül (a tagság ellenőrzése a hívó {@code GroupService} dolga).
     */
    @Transactional
    public PostDto create(UUID authorId, CreatePostRequest request, Group group) {
        User author = userRepository.findById(authorId).orElseThrow(ApiException::userNotFound);

        String content = request.content() == null ? "" : request.content().trim();
        List<PostMedia> media = resolveMedia(request.media());

        // A poszthoz legalább szöveg vagy egy média kell.
        if (content.isEmpty() && media.isEmpty()) {
            throw ApiException.emptyPost();
        }

        Post post = postRepository.save(new Post(author, content, media, group));
        return PostDto.from(post);
    }

    /**
     * Feltöltött média-hivatkozások átalakítása tárolt {@link PostMedia} metaadattá (publikus URL +
     * típus + méret). Csak a poszt-média mappába ({@value #MEDIA_PREFIX}/) mutató kulcsot fogad el.
     * Posztok és hozzászólások (lásd {@code CommentService}) közös média-feloldója.
     */
    public List<PostMedia> resolveMedia(List<MediaItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(item -> {
            if (item.key() == null || !item.key().startsWith(MEDIA_PREFIX + "/")) {
                throw ApiException.invalidUpload();
            }
            return new PostMedia(storageService.publicUrl(item.key()), item.type(), item.sizeBytes());
        }).toList();
    }

    /** Egy bejegyzés szövegének szerkesztése — csak a szerző teheti. */
    @Transactional
    public PostDto update(UUID authorId, UUID postId, String content) {
        Post post = loadOwnPost(authorId, postId);
        String normalized = content == null ? "" : content.trim();
        // Szöveg törölhető, ha a poszton van média; teljesen üres poszt nem maradhat.
        if (normalized.isEmpty() && post.getMedia().isEmpty()) {
            throw ApiException.emptyPost();
        }
        post.setContent(normalized);
        return PostDto.from(post);
    }

    /** Egy bejegyzés törlése — csak a szerző teheti; a csatolt médiafájlok is törlődnek. */
    @Transactional
    public void delete(UUID authorId, UUID postId) {
        deletePostWithMedia(loadOwnPost(authorId, postId));
    }

    /**
     * Egy bejegyzés törlése a jogosultság-ellenőrzés <b>kihagyásával</b>, médiatakarítással.
     * Csak akkor hívható, ha a hívó már engedélyezte a műveletet (pl. csoport-admin moderáció,
     * {@code com.nexa.group.GroupService}).
     */
    @Transactional
    public void deleteAuthorized(Post post) {
        deletePostWithMedia(post);
    }

    /** A média kulcsait a DB-rekord törlése ELŐTT gyűjti ki; a fájltörlés commit után fut. */
    private void deletePostWithMedia(Post post) {
        List<String> mediaKeys = post.getMedia().stream()
                .map(m -> storageService.keyFromPublicUrl(m.getUrl()))
                .toList();
        postRepository.delete(post);
        storageDeleter.deleteAfterCommit(mediaKeys);
    }

    /** Betölti a posztot, és csak akkor adja vissza, ha a hívó a szerzője (különben 404). */
    private Post loadOwnPost(UUID authorId, UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow(ApiException::postNotFound);
        if (!post.getAuthor().getId().equals(authorId)) {
            throw ApiException.postNotFound();
        }
        return post;
    }

    /** Egy felhasználó profil-bejegyzései időrendben (legfrissebb felül, csoport-posztok nélkül). */
    @Transactional(readOnly = true)
    public List<PostDto> listByAuthor(UUID authorId) {
        return postRepository.findByAuthorIdAndGroupIsNullOrderByCreatedAtDesc(authorId)
                .stream()
                .map(PostDto::from)
                .toList();
    }

    /** Egy csoport bejegyzései időrendben (legfrissebb felül, #9). */
    @Transactional(readOnly = true)
    public List<PostDto> listByGroup(UUID groupId) {
        return postRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(PostDto::from)
                .toList();
    }
}
