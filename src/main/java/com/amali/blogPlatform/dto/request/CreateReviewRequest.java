package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Body of POST /api/posts/{postId}/reviews: who reviews, a rating from 1 to 5 and optional text. */
public record CreateReviewRequest(

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be a positive number")
        @Schema(description = "Id of the reviewer", example = "3")
        Long userId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        @Schema(description = "Rating from 1 to 5", example = "5")
        Integer rating,

        @Size(max = 5000, message = "Review text must be at most 5000 characters")
        @Schema(description = "Optional text", example = "Excellent post")
        String reviewText) {
}
