package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.CreatePostRequest;
import com.amali.blogPlatform.dto.request.UpdatePostRequest;
import com.amali.blogPlatform.exception.DuplicateResourceException;
import com.amali.blogPlatform.exception.PostNotFoundException;
import com.amali.blogPlatform.exception.TagNotFoundException;
import com.amali.blogPlatform.exception.UserNotFoundException;
import com.amali.blogPlatform.model.Post;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.repository.CommentRepository;
import com.amali.blogPlatform.repository.PostRepository;
import com.amali.blogPlatform.repository.ReviewRepository;
import com.amali.blogPlatform.repository.TagRepository;
import com.amali.blogPlatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReviewRepository reviewRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, userRepository, tagRepository, commentRepository, reviewRepository);
    }

    private static Post post(long id, long authorId, String title, PostStatus status) {
        return new Post(id, authorId, "alice", title, "content", status, null, null, 0, null, List.of());
    }

    @Test
    void createPostSavesPostThenTagsWithDraftAsDefaultStatus() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(tagRepository.existsById(1L)).thenReturn(true);
        when(postRepository.insert(any())).thenReturn(10L);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post(10, 2, "Hello", PostStatus.DRAFT)));

        postService.createPost(new CreatePostRequest("Hello", "content", 2L, null, List.of(1L)));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).insert(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(PostStatus.DRAFT);
        verify(postRepository).replaceTags(10L, List.of(1L));
    }

    @Test
    void createPostWithUnknownAuthorIsNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> postService.createPost(
                new CreatePostRequest("Hello", "content", 99L, null, null)))
                .isInstanceOf(UserNotFoundException.class);
        verify(postRepository, never()).insert(any());
    }

    @Test
    void createPostWithUnknownTagIsNotFound() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(tagRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> postService.createPost(
                new CreatePostRequest("Hello", "content", 2L, null, List.of(42L))))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void twoPostsCannotHaveTheSameTitle() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(postRepository.existsByTitle("Hello", 0L)).thenReturn(true);   // any author

        assertThatThrownBy(() -> postService.createPost(
                new CreatePostRequest("Hello", "content", 2L, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateWithoutTagsAndStatusKeepsThem() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(post(5, 2, "Old", PostStatus.PUBLISHED)));

        postService.updatePost(5L, new UpdatePostRequest("New title", "new content", null, null));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).update(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(saved.getValue().authorId()).isEqualTo(2L);
        verify(postRepository, never()).replaceTags(anyLong(), any());
    }

    @Test
    void deleteMissingPostIsNotFound() {
        when(postRepository.softDeleteById(5L)).thenReturn(0);

        assertThatThrownBy(() -> postService.deletePost(5L)).isInstanceOf(PostNotFoundException.class);
    }
}
