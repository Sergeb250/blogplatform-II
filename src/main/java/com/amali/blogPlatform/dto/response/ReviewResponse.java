package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Review;

import java.time.LocalDateTime;

/** A review as the API returns it. */
@Schema(description = "A 1-5 star review of a post")
public record ReviewResponse(Long id, Long postId, String postTitle, Long userId, String username, int rating,
                             String reviewText, LocalDateTime createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.id(), review.postId(), review.postTitle(), review.userId(), review.username(),
                review.rating(), review.reviewText(), review.createdAt());
    }
}
