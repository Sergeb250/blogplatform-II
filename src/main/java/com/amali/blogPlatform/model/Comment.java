package com.amali.blogPlatform.model;

import java.time.LocalDateTime;

/** One row of {@code comments}, with the post title and the commenter's username read from joins. */
public record Comment(Long id, Long postId, String postTitle, Long userId, String username, String content,
                      LocalDateTime createdAt) {
}
