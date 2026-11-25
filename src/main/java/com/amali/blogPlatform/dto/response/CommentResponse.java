package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Comment;

import java.time.LocalDateTime;

/** A comment as the API returns it. */
@Schema(description = "A comment on a post")
public record CommentResponse(Long id, Long postId, String postTitle, Long userId, String username, String content,
                              LocalDateTime createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.id(), comment.postId(), comment.postTitle(), comment.userId(), comment.username(),
                comment.content(), comment.createdAt());
    }
}
