package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Role;
import com.amali.blogPlatform.model.User;

import java.time.LocalDateTime;

/** A user as the API returns it. The password hash never leaves the backend. */
@Schema(description = "A user (the password hash is never returned)")
public record UserResponse(Long id, String username, String email, Role role, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.email(), user.role(), user.createdAt());
    }
}
