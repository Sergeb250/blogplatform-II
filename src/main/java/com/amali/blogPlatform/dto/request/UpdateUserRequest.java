package com.amali.blogPlatform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/users/{id}. {@code password} and {@code role} are optional: when not sent the old
 * values stay. Uniqueness is checked in UserService, because the user's own username is allowed.
 */
public record UpdateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may only contain letters, digits, _ . and -")
        @Schema(description = "New login name", example = "alice")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @Schema(description = "New email address", example = "alice@blog.com")
        String email,

        @Size(min = 6, max = 64, message = "Password must be 6 to 64 characters")
        @Schema(description = "New password; leave out to keep the old one", example = "newSecret123")
        String password,

        @Schema(description = "New role; leave out to keep the old one", example = "AUTHOR")
        Role role) {
}
