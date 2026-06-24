package com.nexa.post;

import com.nexa.common.ApiException;
import com.nexa.post.dto.PostDto;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Bejegyzések üzleti logikája. A #5 kártya a létrehozást és a saját profilon
 * való listázást fedi le; a hírfolyam-aggregáció (#10) külön kártya.
 */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PostDto create(UUID authorId, String content) {
        User author = userRepository.findById(authorId).orElseThrow(ApiException::userNotFound);
        Post post = postRepository.save(new Post(author, content.trim()));
        return PostDto.from(post);
    }

    /** Egy felhasználó bejegyzései időrendben (legfrissebb felül). */
    @Transactional(readOnly = true)
    public List<PostDto> listByAuthor(UUID authorId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream()
                .map(PostDto::from)
                .toList();
    }
}
