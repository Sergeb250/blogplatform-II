package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of PUT /api/posts/{id}. The author of a post cannot change.
 * {@code status} null keeps the old status; {@code tagIds} null keeps the old tags
 * (an empty list removes all tags).
 */
public record UpdatePostRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 200, message = "Title must be 3 to 200 characters")
        @Schema(description = "New title", example = "Getting started with Spring Boot 3")
        String title,

        @NotBlank(message = "Content is required")
        @Size(min = 20, message = "Content must be at least 20 characters")
        @Schema(description = "New text, at least 20 characters", example = "Updated text of the post, long enough to pass.")
        String content,

        @Schema(description = "New status; leave out to keep it", example = "PUBLISHED")
        PostStatus status,

        @Schema(description = "New tag ids, e.g. [1]; leave out to keep the tags, [] removes all")
        List<@NotNull @Positive Long> tagIds) {
}
