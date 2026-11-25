package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.model.Role;
import com.amali.blogPlatform.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * SQL for the {@code users} table. Every value is bound to a {@code ?} placeholder.
 * Soft delete: reads return only ACTIVE users ({@code deleted_at IS NULL}).
 */
@Repository
public class UserRepository {

    /** Active users only. It already ends with a WHERE, so callers add conditions with " AND ...". */
    private static final String SELECT_USERS =
            "SELECT user_id, username, email, password_hash, role, created_at FROM users WHERE deleted_at IS NULL";

    /** Turns one row of the result into a {@link User}. */
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(SELECT_USERS + " ORDER BY user_id", USER_ROW_MAPPER);   // active users only
    }

    public Optional<User> findById(long id) {
        return jdbcTemplate.query(SELECT_USERS + " AND user_id = ?", USER_ROW_MAPPER, id)
                .stream().findFirst();
    }

    /** Several users with ONE query: WHERE user_id IN (:ids). Used by GraphQL to load post authors. */
    public List<User> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return namedJdbcTemplate.query(SELECT_USERS + " AND user_id IN (:ids)",
                new MapSqlParameterSource("ids", ids), USER_ROW_MAPPER);
    }

    /** Active user with this username (used by login: a deleted user cannot sign in). */
    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query(SELECT_USERS + " AND username = ?", USER_ROW_MAPPER, username)
                .stream().findFirst();
    }

    /** Active user with this email (used by login). */
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(SELECT_USERS + " AND email = ?", USER_ROW_MAPPER, email)
                .stream().findFirst();
    }

    /** True only for an ACTIVE user; a soft-deleted user counts as "not found". */
    public boolean existsById(long id) {
        return count("SELECT COUNT(*) FROM users WHERE user_id = ? AND deleted_at IS NULL", id) > 0;
    }

    /**
     * True if ANY user (also a deleted one) has this username. A deleted user's row still exists,
     * so the UNIQUE constraint keeps the name reserved, and nobody can take over an old identity.
     */
    public boolean existsByUsername(String username) {
        return count("SELECT COUNT(*) FROM users WHERE username = ?", username) > 0;
    }

    /** True if ANY user (also a deleted one) has this email; see {@link #existsByUsername}. */
    public boolean existsByEmail(String email) {
        return count("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    /** True if another user than {@code userId} (also a deleted one) has this username. */
    public boolean isUsernameTakenByAnother(String username, long userId) {
        return count("SELECT COUNT(*) FROM users WHERE username = ? AND user_id <> ?", username, userId) > 0;
    }

    /** True if another user than {@code userId} (also a deleted one) has this email. */
    public boolean isEmailTakenByAnother(String email, long userId) {
        return count("SELECT COUNT(*) FROM users WHERE email = ? AND user_id <> ?", email, userId) > 0;
    }

    /** Number of active users. */
    public long count() {
        return count("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
    }

    /** Inserts the user and returns its generated {@code user_id}. */
    public long insert(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)";
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.username());
            ps.setString(2, user.email());
            ps.setString(3, user.passwordHash());
            ps.setString(4, user.role().name());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Updates username, email, password hash and role; returns the number of rows changed (0 or 1). */
    public int update(User user) {
        return jdbcTemplate.update(
                "UPDATE users SET username = ?, email = ?, password_hash = ?, role = ? WHERE user_id = ? AND deleted_at IS NULL",
                user.username(), user.email(), user.passwordHash(), user.role().name(), user.id());
    }

    /**
     * Soft delete: marks the user as deleted instead of removing the row. Returns 0 when the user does
     * not exist or is already deleted. Their posts, comments and reviews are soft-deleted by the service.
     */
    public int softDeleteById(long id) {
        return jdbcTemplate.update(
                "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ? AND deleted_at IS NULL", id);
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }
}
