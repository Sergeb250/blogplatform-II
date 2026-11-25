package com.amali.blogPlatform.model;

import java.time.LocalDateTime;

/** One row of {@code reviews} (a 1-5 rating and optional text, one per user per post). */
public record Review(Long id, Long postId, String postTitle, Long userId, String username, int rating, String reviewText,
                     LocalDateTime createdAt) {
}
