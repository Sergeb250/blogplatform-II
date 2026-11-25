package com.amali.blogPlatform.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One row of {@code posts}, plus values the SELECT reads from other tables: the author's username
 * (join with {@code users}), the number of comments, the average rating (null when there are no
 * reviews) and the tags (from {@code post_tags}, loaded with one extra query for all posts).
 */
public record Post(Long id, Long authorId, String authorUsername, String title, String content,
                   PostStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                   int commentCount, Double averageRating, List<Tag> tags) {

    public Post {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /** The same post with its tags set. */
    public Post withTags(List<Tag> newTags) {
        return new Post(id, authorId, authorUsername, title, content, status, createdAt, updatedAt,
                commentCount, averageRating, newTags);
    }
}
