package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of PUT /api/comments/{id}: only the text of a comment can change. */
public record UpdateCommentRequest(

        @NotBlank(message = "Comment content is required")
        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        @Schema(description = "New comment text", example = "Great introduction, thanks a lot!")
        String content) {
}
