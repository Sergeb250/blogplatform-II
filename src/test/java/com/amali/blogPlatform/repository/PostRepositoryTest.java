package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.config.DatabaseConfig;
import com.amali.blogPlatform.model.Post;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the real SQL against H2 loaded with test data.sql; every test is rolled back afterwards. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DatabaseConfig.class, PostRepository.class, CommentRepository.class, ReviewRepository.class})
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findAllReturnsNewestFirstWithTags() {
        List<Post> posts = postRepository.findAll();

        assertThat(posts).extracting(Post::id).containsExactly(3L, 2L, 1L);
        Post first = posts.get(2);
        assertThat(first.tags()).extracting(Tag::name).containsExactly("Java", "Spring");
        assertThat(posts.get(0).tags()).isEmpty();
    }

    @Test
    void findByIdReadsJoinedAuthorCountsAndAverageRating() {
        Post post = postRepository.findById(1).orElseThrow();

        assertThat(post.title()).isEqualTo("Getting started with Spring Boot");
        assertThat(post.authorUsername()).isEqualTo("alice");
        assertThat(post.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.commentCount()).isEqualTo(2);
        assertThat(post.averageRating()).isEqualTo(4.5);
    }

    @Test
    void postWithoutReviewsHasNullAverage() {
        assertThat(postRepository.findById(3).orElseThrow().averageRating()).isNull();
    }

    @Test
    void findByIdOfMissingPostIsEmpty() {
        assertThat(postRepository.findById(999)).isEmpty();
        assertThat(postRepository.existsById(999)).isFalse();
    }

    @Test
    void insertReturnsGeneratedKeyAndReplaceTagsUsesBatch() {
        long id = postRepository.insert(new Post(null, 2L, null, "A new post", "Some content here",
                PostStatus.DRAFT, null, null, 0, null, null));
        postRepository.replaceTags(id, List.of(2L, 3L));

        Post saved = postRepository.findById(id).orElseThrow();
        assertThat(saved.title()).isEqualTo("A new post");
        assertThat(saved.tags()).extracting(Tag::name).containsExactly("Database", "Spring");

        postRepository.replaceTags(id, List.of());
        assertThat(postRepository.findTagsByPostId(id)).isEmpty();
    }

    @Test
    void updateChangesColumns() {
        Post post = postRepository.findById(3).orElseThrow();
        int changed = postRepository.update(new Post(post.id(), post.authorId(), null, "Draft notes v2",
                post.content(), PostStatus.PUBLISHED, null, null, 0, null, null));

        assertThat(changed).isEqualTo(1);
        Post updated = postRepository.findById(3).orElseThrow();
        assertThat(updated.title()).isEqualTo("Draft notes v2");
        assertThat(updated.status()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    void softDeleteHidesThePostButKeepsTheRow() {
        assertThat(postRepository.softDeleteById(1)).isEqualTo(1);
        assertThat(postRepository.softDeleteById(1)).isEqualTo(0);                  // already deleted

        assertThat(postRepository.findById(1)).isEmpty();
        assertThat(postRepository.existsById(1)).isFalse();
        assertThat(postRepository.findAll()).extracting(Post::id).containsExactly(3L, 2L);
        assertThat(jdbcTemplate.queryForObject(                                       // the row is still there
                "SELECT COUNT(*) FROM posts WHERE post_id = 1 AND deleted_at IS NOT NULL", Long.class)).isEqualTo(1L);
    }

    @Test
    void softDeletedCommentsAndReviewsAreNotCounted() {
        commentRepository.softDeleteByPostId(1);
        reviewRepository.softDeleteById(reviewRepository.findByPostId(1).get(0).id());

        Post post = postRepository.findById(1).orElseThrow();
        assertThat(post.commentCount()).isZero();
        assertThat(post.averageRating()).isEqualTo(5.0);                              // only the remaining review
    }
}
