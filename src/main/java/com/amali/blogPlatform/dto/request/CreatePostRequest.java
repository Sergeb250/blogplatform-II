package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.validation.UniquePostTitle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of POST /api/posts. {@code status} is optional (DRAFT when not sent);
 * {@code tagIds} is optional (no tags when not sent).
 */
public record CreatePostRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 200, message = "Title must be 3 to 200 characters")
        @UniquePostTitle
        @Schema(description = "Post title, 3-200 characters", example = "Getting started with Spring Boot")
        String title,

        @NotBlank(message = "Content is required")
        @Size(min = 20, message = "Content must be at least 20 characters")
        @Schema(description = "Post text, at least 20 characters", example = "Spring Boot makes Java web development quick and simple.")
        String content,

        @NotNull(message = "Author ID is required")
        @Positive(message = "Author ID must be a positive number")
        @Schema(description = "Id of an existing user", example = "2")
        Long authorId,

        @Schema(description = "DRAFT or PUBLISHED; DRAFT when not sent", example = "PUBLISHED")
        PostStatus status,

        @Schema(description = "Ids of existing tags, e.g. [1, 2]")
        List<@NotNull @Positive Long> tagIds) {
}
