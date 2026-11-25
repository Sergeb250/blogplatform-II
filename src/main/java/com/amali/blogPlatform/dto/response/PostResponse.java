package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Post;
import com.amali.blogPlatform.model.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

/** A post as the API returns it, with its author, tags, comment count and average rating. */
@Schema(description = "A blog post with its author, tags, comment count and average rating")
public record PostResponse(Long id, String title, String content, PostStatus status,
                           Long authorId, String authorUsername, List<TagResponse> tags,
                           int commentCount, Double averageRating,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.id(), post.title(), post.content(), post.status(),
                post.authorId(), post.authorUsername(),
                post.tags().stream().map(TagResponse::from).toList(),
                post.commentCount(), post.averageRating(),
                post.createdAt(), post.updatedAt());
    }
}
