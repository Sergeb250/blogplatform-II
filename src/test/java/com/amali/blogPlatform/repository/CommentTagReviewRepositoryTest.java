package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.config.DatabaseConfig;
import com.amali.blogPlatform.model.Comment;
import com.amali.blogPlatform.model.Review;
import com.amali.blogPlatform.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DatabaseConfig.class, CommentRepository.class, TagRepository.class, ReviewRepository.class})
class CommentTagReviewRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void commentsOfPostOldestFirstWithUsername() {
        assertThat(commentRepository.findByPostId(1))
                .extracting(Comment::username).containsExactly("bob", "admin");
    }

    @Test
    void commentInsertUpdateDelete() {
        long id = commentRepository.insert(new Comment(null, 2L, null, 1L, null, "Nice post", null));
        commentRepository.updateContent(id, "Very nice post");
        assertThat(commentRepository.findById(id).orElseThrow().content()).isEqualTo("Very nice post");
        assertThat(commentRepository.softDeleteById(id)).isEqualTo(1);
        assertThat(commentRepository.softDeleteById(id)).isEqualTo(0);              // already deleted
        assertThat(commentRepository.findById(id)).isEmpty();
    }

    @Test
    void tagsByNameAndCrud() {
        assertThat(tagRepository.findAll()).extracting(Tag::name).containsExactly("Database", "Java", "Spring");
        assertThat(tagRepository.findByName("Java")).map(Tag::id).contains(1L);

        long id = tagRepository.insert(new Tag(null, "GraphQL"));
        tagRepository.update(new Tag(id, "GraphQL API"));
        assertThat(tagRepository.findById(id).orElseThrow().name()).isEqualTo("GraphQL API");
        assertThat(tagRepository.softDeleteById(id)).isEqualTo(1);
        assertThat(tagRepository.findById(id)).isEmpty();                            // hidden
        assertThat(tagRepository.restoreDeletedByName("GraphQL API")).contains(id);   // same id back
    }

    @Test
    void reviewsOfPostAndOnePerUser() {
        assertThat(reviewRepository.findByPostId(1)).extracting(Review::rating).containsExactly(4, 5);
        assertThat(reviewRepository.existsByPostIdAndUserId(1, 3)).isTrue();
        assertThat(reviewRepository.existsByPostIdAndUserId(3, 3)).isFalse();

        long id = reviewRepository.insert(new Review(null, 3L, null, 3L, null, 2, "Meh", null));
        reviewRepository.update(id, 4, "Better after the edit");
        Review updated = reviewRepository.findById(id).orElseThrow();
        assertThat(updated.rating()).isEqualTo(4);
        assertThat(updated.username()).isEqualTo("bob");
    }
}
