package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.model.Review;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL for the {@code reviews} table (one review per user per post, rating 1-5).
 * Soft delete: reads return only ACTIVE reviews.
 */
@Repository
public class ReviewRepository {

    /** Active reviews only. It already ends with a WHERE, so callers add conditions with " AND ...". */
    private static final String SELECT_REVIEWS = """
            SELECT r.review_id, r.post_id, p.title AS post_title, r.user_id, u.username, r.rating, r.review_text, r.created_at
            FROM reviews r
            JOIN users u ON u.user_id = r.user_id
            JOIN posts p ON p.post_id = r.post_id
            WHERE r.deleted_at IS NULL
            """;

    private static final RowMapper<Review> REVIEW_ROW_MAPPER = (rs, rowNum) -> new Review(
            rs.getLong("review_id"),
            rs.getLong("post_id"),
            rs.getString("post_title"),
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getInt("rating"),
            rs.getString("review_text"),
            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public ReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** The reviews of a post, newest first. */
    public List<Review> findByPostId(long postId) {
        return jdbcTemplate.query(
                SELECT_REVIEWS + " AND r.post_id = ? ORDER BY r.created_at DESC, r.review_id DESC",
                REVIEW_ROW_MAPPER, postId);
    }

    /**
     * Every row, or only those written by {@code userId}, or only those on posts written by
     * {@code postAuthorId} (null = no filter). Newest first.
     */
    public List<Review> findAll(Long userId, Long postAuthorId) {
        List<Object> values = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_REVIEWS);
        if (userId != null) {
            sql.append(" AND r.user_id = ?");
            values.add(userId);
        }
        if (postAuthorId != null) {
            sql.append(" AND p.author_id = ?");
            values.add(postAuthorId);
        }
        sql.append(" ORDER BY r.created_at DESC, r.review_id DESC");
        return jdbcTemplate.query(sql.toString(), REVIEW_ROW_MAPPER, values.toArray());
    }

    public Optional<Review> findById(long id) {
        return jdbcTemplate.query(SELECT_REVIEWS + " AND r.review_id = ?", REVIEW_ROW_MAPPER, id)
                .stream().findFirst();
    }

    /** True if the user has an ACTIVE review of the post. */
    public boolean existsByPostIdAndUserId(long postId, long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE post_id = ? AND user_id = ? AND deleted_at IS NULL",
                Long.class, postId, userId);
        return count != null && count > 0;
    }

    /**
     * The table allows one review per user per post (UNIQUE), and a soft-deleted review still holds that
     * place. So when the user reviews the post again, the old row is brought back with the new rating
     * and text. Returns the id of the restored review, or empty when there was no deleted review.
     */
    public Optional<Long> restoreDeleted(long postId, long userId, int rating, String reviewText) {
        int restored = jdbcTemplate.update("""
                UPDATE reviews SET deleted_at = NULL, rating = ?, review_text = ?, created_at = CURRENT_TIMESTAMP
                WHERE post_id = ? AND user_id = ? AND deleted_at IS NOT NULL
                """, rating, reviewText, postId, userId);
        if (restored == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT review_id FROM reviews WHERE post_id = ? AND user_id = ?", Long.class, postId, userId));
    }

    /** Inserts the review and returns its generated {@code review_id}. */
    public long insert(Review review) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO reviews (post_id, user_id, rating, review_text) VALUES (?, ?, ?, ?)",
                    new String[]{"review_id"});
            ps.setLong(1, review.postId());
            ps.setLong(2, review.userId());
            ps.setInt(3, review.rating());
            ps.setString(4, review.reviewText());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Rating and text can change; the post and the reviewer cannot. */
    public int update(long id, int rating, String reviewText) {
        return jdbcTemplate.update(
                "UPDATE reviews SET rating = ?, review_text = ? WHERE review_id = ? AND deleted_at IS NULL",
                rating, reviewText, id);
    }

    /** Soft delete: marks the review as deleted. Returns 0 when it does not exist or is already deleted. */
    public int softDeleteById(long id) {
        return jdbcTemplate.update(
                "UPDATE reviews SET deleted_at = CURRENT_TIMESTAMP WHERE review_id = ? AND deleted_at IS NULL", id);
    }

    /** Soft-deletes every active review of a post (used when the post is deleted). */
    public int softDeleteByPostId(long postId) {
        return jdbcTemplate.update(
                "UPDATE reviews SET deleted_at = CURRENT_TIMESTAMP WHERE post_id = ? AND deleted_at IS NULL", postId);
    }

    /**
     * Soft-deletes every active review written by the user OR on a post written by the user
     * (used when the user is deleted).
     */
    public int softDeleteByUserId(long userId) {
        return jdbcTemplate.update("""
                UPDATE reviews SET deleted_at = CURRENT_TIMESTAMP
                WHERE deleted_at IS NULL
                  AND (user_id = ? OR post_id IN (SELECT post_id FROM posts WHERE author_id = ?))
                """, userId, userId);
    }
}
