package com.amali.blogPlatform.model;

import java.time.LocalDateTime;

/**
 * One row of {@code users}. A plain record (not a JPA entity): the repository builds it from a
 * {@link java.sql.ResultSet} with a {@code RowMapper}. {@code id} is null until the user is saved.
 */
public record User(Long id, String username, String email, String passwordHash, Role role,
                   LocalDateTime createdAt) {
}
