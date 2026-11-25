package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Role;
import com.amali.blogPlatform.validation.UniqueEmail;
import com.amali.blogPlatform.validation.UniqueUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/users. {@code role} is optional: READER when not sent.
 * The plain password is hashed by the service before it is stored.
 */
public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may only contain letters, digits, _ . and -")
        @UniqueUsername
        @Schema(description = "Unique login name (letters, digits, _ . and -)", example = "alice")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @UniqueEmail
        @Schema(description = "Unique email address", example = "alice@blog.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 64, message = "Password must be 6 to 64 characters")
        @Schema(description = "Plain password, 6-64 characters; stored only as a BCrypt hash", example = "secret123")
        String password,

        @Schema(description = "ADMIN, AUTHOR or READER; READER when not sent", example = "AUTHOR")
        Role role) {
}
