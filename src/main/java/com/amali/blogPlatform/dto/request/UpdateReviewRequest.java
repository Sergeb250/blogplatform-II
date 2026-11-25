package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of PUT /api/reviews/{id}: the rating and the text can change. */
public record UpdateReviewRequest(

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        @Schema(description = "New rating from 1 to 5", example = "4")
        Integer rating,

        @Size(max = 5000, message = "Review text must be at most 5000 characters")
        @Schema(description = "New text", example = "Very good")
        String reviewText) {
}
