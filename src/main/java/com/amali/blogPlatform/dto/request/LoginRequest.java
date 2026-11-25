package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/auth/login. */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        @Schema(description = "Username or email address", example = "alice")
        String username,

        @NotBlank(message = "Password is required")
        @Schema(description = "Password", example = "secret123")
        String password) {
}
