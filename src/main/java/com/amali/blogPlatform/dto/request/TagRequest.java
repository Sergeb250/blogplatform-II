package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/tags and PUT /api/tags/{id}. */
public record TagRequest(

        @NotBlank(message = "Tag name is required")
        @Size(max = 50, message = "Tag name must be at most 50 characters")
        @Schema(description = "Tag name, unique", example = "Java")
        String name) {
}
