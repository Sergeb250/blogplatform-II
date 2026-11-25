package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Body of POST /api/posts/{postId}/comments: who writes the comment and its text. */
public record CreateCommentRequest(

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be a positive number")
        @Schema(description = "Id of the user who writes the comment", example = "3")
        Long userId,

        @NotBlank(message = "Comment content is required")
        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        @Schema(description = "Comment text", example = "Great introduction, thanks!")
        String content) {
}
